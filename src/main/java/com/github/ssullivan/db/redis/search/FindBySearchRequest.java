package com.github.ssullivan.db.redis.search;

import static com.github.ssullivan.db.redis.RedisConstants.DEFAULT_EXPIRE_SECONDS;
import static com.github.ssullivan.db.redis.RedisConstants.indexByGeoKey;

import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.redis.IAsyncRedisConnectionPool;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.db.redis.ToFacilityWithRadiusConverter;
import com.github.ssullivan.model.AvailableServices;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.GeoPoint;
import com.github.ssullivan.model.GeoRadiusCondition;
import com.github.ssullivan.model.GeoUnit;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchRequest;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.ServicesCondition;
import com.github.ssullivan.model.SetOperation;
import com.github.ssullivan.model.collections.Tuple2;
import com.github.ssullivan.utils.ShortUuid;
import io.lettuce.core.GeoRadiusStoreArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FindBySearchRequest extends AbstractFindFacility {
  private static final Logger LOGGER = LoggerFactory.getLogger(FindBySearchRequest.class);

  @Inject
  public FindBySearchRequest(IRedisConnectionPool redis,
      IAsyncRedisConnectionPool asyncPool,
      IFacilityDao facilityDao, IFeedDao feedDao) {
    super(redis, asyncPool, facilityDao, feedDao);
  }

  public CompletionStage<SearchResults<Facility>> find(SearchRequest searchRequest,
      Page page) throws Exception {

    Long totalResults = 0L;
    List<String> facilityIdentifiers = new ArrayList<>();

    try (StatefulRedisConnection<String, String> connection = syncPool.borrowConnection()) {
      RedisCommands<String, String> sync = connection.sync();

      final Optional<String> feedOption = this.feedDao.searchFeedId();

      if (!feedOption.isPresent()) {
        LOGGER.error("Error connecting to the backend database! Unable to find current feed");
        return CompletableFuture.completedFuture(SearchResults.empty());
      }
      final String searchKey = "s:" + ShortUuid.randomShortUuid() + ":";

      final Tuple2<Long, String> serviceCodeResults =
          findByServiceCodeConditions(sync, searchKey, searchRequest.getFinalSetOperation(), searchRequest.getConditions());

      LOGGER.debug("Found {} matching locations for the service code conditions", serviceCodeResults.get_1());

      totalResults = serviceCodeResults.get_1();
      if (searchRequest.getGeoRadiusCondition() != null) {
        final Tuple2<Long, String> geoResult = findByGeoPoint(sync, feedOption.get(),
            searchKey, searchRequest.getGeoRadiusCondition());

        LOGGER.debug("");

        if (serviceCodeResults.get_1() > 0) {
          totalResults = sync.zinterstore(serviceCodeResults.get_2(), serviceCodeResults.get_2(),
              geoResult.get_2());
        }
        else {
          totalResults = sync.zunionstore(serviceCodeResults.get_2(), serviceCodeResults.get_2(),
              geoResult.get_2());
        }
      }

      if (totalResults == null) {
        totalResults = 0L;
      }

      final List<String> results = sync
          .zrange(serviceCodeResults.get_2(), page.offset(), page.offset() + page.size());
      facilityIdentifiers.addAll(results);
    }


    final Long totalFound = totalResults;

    final Function<List<Facility>, List<Facility>> toFacilityWithRadius =
        applyToFacilityWithRadius(searchRequest);


    return this.facilityDao.fetchBatchAsync(facilityIdentifiers)
        .thenApply(this::applyAvailableServicesAll)
        .thenApply(toFacilityWithRadius)
        .thenApply(it -> SearchResults.searchResults(totalFound, it));
  }

  private Tuple2<Long, String> findByGeoPoint(final RedisCommands<String, String> sync,
      final String feedKey,
      final String searchKey,
      final GeoRadiusCondition geoRadiusCondition) {

    Objects.requireNonNull(sync, "Redis connection must not be null");
    Objects.requireNonNull(searchKey, "Search key must not be null");
    Objects.requireNonNull(geoRadiusCondition, "Geo Condition  must not be null");


    Long totalResults = sync
        .georadius(indexByGeoKey(feedKey), geoRadiusCondition.getGeoPoint()
                .lon(), geoRadiusCondition.getGeoPoint().lat(),
            geoRadiusCondition.getRadius(), geoRadiusCondition.getGeoUnit().unit(),
            GeoRadiusStoreArgs.Builder
                .withStoreDist(searchKey + "geo"));

    if (totalResults == null) {
      totalResults = 0L;
    }
    LOGGER.debug("Found {} locations within {} {} of {}", totalResults, geoRadiusCondition.getRadius(),
        geoRadiusCondition.getGeoUnit(), geoRadiusCondition.getGeoPoint());

    sync.expire(searchKey + "geo", DEFAULT_EXPIRE_SECONDS);
    return new Tuple2<>(totalResults, searchKey + "geo");
  }

  /**
   * Find locations by the provided {@link ServicesCondition}'s.
   *
   * @param sync connection to redis
   * @param searchKey the search key prefix
   * @param setOperation an final set operation to perfrom across all the result sets
   * @param conditions the user provided search conditions to filter by
   * @return an instance of {@link Tuple2} wher the first item is the number of results, and the
   *         second result is the key where the final results are stored
   */
  private Tuple2<Long, String> findByServiceCodeConditions(final RedisCommands<String, String> sync,
      final String searchKey,
      final SetOperation setOperation,
      final List<ServicesCondition> conditions) throws IOException {

    Objects.requireNonNull(sync, "Redis connection must not be null");
    Objects.requireNonNull(searchKey, "Search key must not be null");
    Objects.requireNonNull(setOperation, "Set operation must not be null");
    Objects.requireNonNull(conditions, "ServicesConditions must not be null");


    final String[] conditionKeys = new String[conditions.size()];
    int i = 0;
    for (ServicesCondition servicesCondition : conditions) {
      conditionKeys[i] = searchKey + i;
      long totalFound = findByServiceConditions(sync, conditionKeys[i], servicesCondition);
      i++;
    }


    Long totalFound = 0L;
    if (!conditions.isEmpty()) {
      switch (setOperation) {
        case INTERSECTION:
          totalFound = sync.zinterstore(searchKey + ":f", conditionKeys);
          break;
        case UNION:
          totalFound = sync.zunionstore(searchKey + ":f", conditionKeys);
        default:
      }

      sync.del(conditionKeys);
    }

    if (totalFound == null) {
      totalFound = 0L;
    }
    return new Tuple2<>(totalFound, searchKey + ":f");
  }


  private long findByServiceConditions(final RedisCommands<String, String> sync, final String searchKey, final ServicesCondition servicesCondition)
      throws IOException {
    /**
     * Locations are are indexed by service code. There are multiple redis sets
     * organized like servicecode -> [ facility 1, facility 2, etc.. ]
     *
     * Based on the user's query we are figuring which of these service code indices we need to
     * query.
     */
    final String[] services = getServiceCodeIndices(feedDao.searchFeedId().orElse(""), servicesCondition.getServices());
    Long totalFound = 0L;
    switch (servicesCondition.getMatchOperator()) {
      case MUST:
        totalFound = sync.sinterstore(searchKey, services);
        break;
      case SHOULD:
        totalFound = sync.sunionstore(searchKey, services);
        break;
      default:
    }
    sync.expire(searchKey, DEFAULT_EXPIRE_SECONDS);

    if (totalFound == null) {
      totalFound = 0L;
    }

    LOGGER.debug("[{}] Found {} services matching {}", searchKey, totalFound, servicesCondition.getServices());

    // searchKey now contains a all of the ids for the locations we are looking for
    final String[] mustNotServices = getServiceCodeIndices(servicesCondition.getMustNotServiceCodes());
    Long totalDiff = 0L;
    if (totalFound > 0 && mustNotServices.length > 0) {
      sync.sunionstore(searchKey + ":!", mustNotServices);
      totalDiff = sync.sdiffstore(searchKey, searchKey, searchKey + ":!");
      sync.expire(searchKey + ":!", DEFAULT_EXPIRE_SECONDS);
      sync.del(searchKey + ":!");

      if (totalDiff == null) {
        totalDiff = 0L;
      }

      LOGGER.debug("[{}] Found {} services matching {} after diff", searchKey, totalDiff, servicesCondition.getServices());
      return totalDiff;
    }

    return totalFound;
  }


  private List<Facility> applyAvailableServicesAll(final List<Facility> facilities) {
    for (Facility facility : facilities) {
      applyAvailableServices(facility);
    }
    return facilities;
  }

  private Facility applyAvailableServices(final Facility facility) {
    final AvailableServices availableServices = facilityDao.getAvailableServices(facility);
    facility.setAvailableServices(availableServices);
    return facility;
  }

  /**
   * Higher order function to return a function that will change Facility to FacilityWithRadius.
   *
   * @param searchRequest
   * @return
   */
  private Function<List<Facility>, List<Facility>> applyToFacilityWithRadius(final SearchRequest searchRequest) {

    final GeoPoint geoPoint = searchRequest.getGeoRadiusCondition() != null ? searchRequest.getGeoRadiusCondition().getGeoPoint() : null;
    final GeoUnit geoUnit = searchRequest.getGeoRadiusCondition() != null ? searchRequest.getGeoRadiusCondition().getGeoUnit() : GeoUnit.MILE;

    final ToFacilityWithRadiusConverter ToFacilityWithRadius = new ToFacilityWithRadiusConverter(geoPoint, geoUnit.getAbbrev());

    return facilities1 -> facilities1.stream().map(ToFacilityWithRadius).collect(Collectors.toList());
  }
}
