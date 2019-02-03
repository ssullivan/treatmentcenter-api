package com.github.ssullivan.db.redis.search;

import static com.github.ssullivan.db.redis.RedisConstants.DEFAULT_EXPIRE_SECONDS;

import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IFindBySearchRequest;
import com.github.ssullivan.db.redis.IAsyncRedisConnectionPool;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.db.redis.ToFacilityWithRadiusConverter;
import com.github.ssullivan.model.AvailableServices;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchRequest;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.ServicesCondition;
import com.github.ssullivan.utils.ShortUuid;
import com.spotify.futures.CompletableFutures;
import io.lettuce.core.ZStoreArgs;
import io.lettuce.core.api.async.RedisAsyncCommands;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FindBySearchRequest extends AbstractFindFacility implements IFindBySearchRequest {
  private static final Logger LOGGER = LoggerFactory.getLogger(FindBySearchRequest.class);

  @Inject
  public FindBySearchRequest(IRedisConnectionPool redis,
      IAsyncRedisConnectionPool asyncPool,
      IFacilityDao facilityDao,
      IFeedDao feedDao) {
    super(redis, asyncPool, facilityDao, feedDao);
  }


  public CompletionStage<SearchResults<Facility>> find(SearchRequest searchRequest,
      Page page) throws Exception {

    if (null == searchRequest) {
      LOGGER.warn("Invalid Search Request of null. No Results!");
      return CompletableFuture.completedFuture(SearchResults.searchResults(0));
    }


    return asyncPool.borrowConnection()
        .thenCompose(connection -> {
          try {
            return find(connection.async(), searchRequest, page)
                .whenComplete((s, throwable) -> asyncPool.relase(connection));
          } catch (IOException e) {
            e.printStackTrace();
          }

          return CompletableFuture.completedFuture(SearchResults.searchResults(0, new ArrayList<>()));
        });
  }

  private CompletionStage<SearchResults<Facility>> find(RedisAsyncCommands<String, String> async, SearchRequest searchRequest, Page page)
      throws IOException {


    final Optional<String> feedOption = this.feedDao.currentFeedId();

    if (!feedOption.isPresent()) {
      LOGGER.error("Error connecting to the backend database! Unable to find current feed");
      return CompletableFuture.completedFuture(SearchResults.empty());
    }
    final String searchKey = "s:" + ShortUuid.randomShortUuid() + ":";
    final String resultKey = searchKey + ":0";
    final String geoKey = searchKey + ":geo";

    async.setAutoFlushCommands(false);


    // Perform the following operations in Batch
    boolean hasResultKey = false;
    final List<ServicesCondition> servicesConditions = searchRequest.getConditions();

    if (!servicesConditions.isEmpty()) {
      int counter = 0;

      final String[] keys = new String[servicesConditions.size()];
      for (final ServicesCondition servicesCondition : servicesConditions) {
        final String key = searchKey + "s:" + counter;
        if (!createServiceSearchSet(async, key, feedOption.orElse(""), servicesCondition)) {
          LOGGER.warn("Failed to create search set: {}", key);
        }
        keys[counter] = key;
        counter = counter + 1;
      }

      switch (searchRequest.getFinalSetOperation()) {
        case INTERSECTION:
          async.sinterstore(resultKey, keys);
        case UNION:
          async.sunionstore(resultKey, keys);
        default:
          async.sinterstore(resultKey, keys);
      }

      async.del(keys);
      hasResultKey = true;
    }

    final boolean hasGeoSet = createGeoSearchSet(async,
        geoKey,
        feedOption.orElse(""),
        searchRequest.getGeoRadiusCondition());

    CompletionStage<Long> totalSearchResultsFuture = CompletableFuture.completedFuture(0L);
    if (hasResultKey && hasGeoSet) {

      async.zinterstore(searchKey, ZStoreArgs.Builder.weights(1.0), resultKey);

      // #3 Find the intersection of the places that have our services we want and
      //    are within a specific radius
      totalSearchResultsFuture = async.zinterstore(searchKey, searchKey, geoKey);
      async.expire(searchKey, DEFAULT_EXPIRE_SECONDS);
      async.del(resultKey, geoKey);
    }
    else if (hasResultKey) {
      totalSearchResultsFuture = async
          .zinterstore(searchKey, ZStoreArgs.Builder.weights(1.0), resultKey);
      async.expire(searchKey, DEFAULT_EXPIRE_SECONDS);
      async.del(resultKey);
    }
    else if (hasGeoSet) {
      totalSearchResultsFuture = async.zinterstore(searchKey, geoKey);
      async.expire(searchKey, DEFAULT_EXPIRE_SECONDS);
      async.del(geoKey);
    }

    async.flushCommands();
    async.setAutoFlushCommands(true);


    // #4 Fetch the results
    final CompletionStage<List<String>> idFutures = async
        .zrange(searchKey, page.offset(), page.offset() + page.size());
    async.getStatefulConnection().setAutoFlushCommands(true);

    final FetchFacilities fetchFacilities = new FetchFacilities(searchRequest, facilityDao, hasGeoSet);

    return CompletableFutures.combineFutures(idFutures, totalSearchResultsFuture, fetchFacilities::apply)
        .whenComplete((result, error) -> {
          if (error != null) {
            LOGGER.error("An error occurred while processing search", error);
          }
          // #6 Explicitly delete the keys
          async.del(searchKey);
          async.getStatefulConnection().setAutoFlushCommands(true);
          async.getStatefulConnection().close();
        });
  }

  private static class FetchFacilities implements BiFunction<Collection<String>, Long, CompletionStage<SearchResults<Facility>>> {
    private IFacilityDao facilityDao;
    private SearchRequest searchRequest;
    private boolean hasGeoSet;

    FetchFacilities(SearchRequest searchRequest, IFacilityDao facilityDao, boolean hasGeoSet) {
      this.searchRequest = searchRequest;
      this.hasGeoSet = hasGeoSet;
      this.facilityDao = facilityDao;
    }


    @Override
    public CompletionStage<SearchResults<Facility>> apply(Collection<String> ids, Long totalResults) {
      return facilityDao.fetchBatchAsync(ids)
          .thenApply(facilities -> facilities
              .stream()
              .peek(facility -> {
                final AvailableServices availableServices = facilityDao.getAvailableServices(facility);
                facility.setAvailableServices(availableServices);
              })
              .map(facility -> {
                if (hasGeoSet) {
                  final double latitude = searchRequest.getGeoRadiusCondition().getGeoPoint()
                      .lat();
                  final double longitude = searchRequest.getGeoRadiusCondition().getGeoPoint()
                      .lon();
                  final String geoUnit = searchRequest.getGeoRadiusCondition().getGeoUnit()
                      .getAbbrev();


                  final ToFacilityWithRadiusConverter converter = new ToFacilityWithRadiusConverter(latitude, longitude, geoUnit);
                  return converter.apply(facility);
                }

                return facility;
              })
              .collect(Collectors.toList()))
          .thenApply(facilities -> SearchResults.searchResults(totalResults, facilities));
    }
  }

}
