package com.github.ssullivan.db.redis.search;

import static com.github.ssullivan.db.redis.RedisConstants.DEFAULT_EXPIRE_SECONDS;

import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.redis.IAsyncRedisConnectionPool;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchRequest;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.ServicesCondition;
import com.github.ssullivan.utils.ShortUuid;
import io.lettuce.core.ZStoreArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.RedisCommand;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FindBySearchRequestSync extends AbstractFindFacility {
  private static final Logger LOGGER = LoggerFactory.getLogger(FindBySearchRequest.class);

  public FindBySearchRequestSync(IRedisConnectionPool redis,
      IAsyncRedisConnectionPool asyncPool,
      IFacilityDao facilityDao, IFeedDao feedDao) {
    super(redis, asyncPool, facilityDao, feedDao);
  }

  public CompletionStage<SearchResults<Facility>> find(SearchRequest searchRequest,
      Page page) throws Exception {

    try (StatefulRedisConnection<String, String> connection = syncPool.borrowConnection()) {
      RedisCommands<String, String> sync = connection.sync();

      final Optional<String> feedOption = this.feedDao.currentFeedId();

      if (!feedOption.isPresent()) {
        LOGGER.error("Error connecting to the backend database! Unable to find current feed");
        return CompletableFuture.completedFuture(SearchResults.empty());
      }
      final String searchKey = "s:" + ShortUuid.randomShortUuid() + ":";
      final String resultKey = searchKey + ":0";
      final String geoKey = searchKey + ":geo";

      // Perform the following operations in Batch
      boolean hasResultKey = false;
      final List<ServicesCondition> servicesConditions = searchRequest.getConditions();

      if (!servicesConditions.isEmpty()) {
        int counter = 0;

        final String[] keys = new String[servicesConditions.size()];
        for (final ServicesCondition servicesCondition : servicesConditions) {
          final String key = searchKey + "s:" + counter;
          if (!createServiceSearchSet(connection.sync(), key, feedOption.orElse(""), servicesCondition)) {
            LOGGER.warn("Failed to create search set: {}", key);
          }
          keys[counter] = key;
          counter = counter + 1;
        }

        switch (searchRequest.getFinalSetOperation()) {
          case INTERSECTION:
            connection.sync().sinterstore(resultKey, keys);
          case UNION:
            sync.sunionstore(resultKey, keys);
          default:
            sync.sinterstore(resultKey, keys);
        }

        sync.del(keys);
        hasResultKey = true;
      }

      final boolean hasGeoSet = createGeoSearchSet(sync,
          geoKey,
          feedOption.orElse(""),
          searchRequest.getGeoRadiusCondition());

      long totalSearchResultsFuture = 0;
      if (hasResultKey && hasGeoSet) {

        sync.zinterstore(searchKey, ZStoreArgs.Builder.weights(1.0), resultKey);

        // #3 Find the intersection of the places that have our services we want and
        //    are within a specific radius
        totalSearchResultsFuture = sync.zinterstore(searchKey, searchKey, geoKey);
        sync.expire(searchKey, DEFAULT_EXPIRE_SECONDS);
        sync.del(resultKey, geoKey);
      }
      else if (hasResultKey) {
        totalSearchResultsFuture = sync
            .zinterstore(searchKey, ZStoreArgs.Builder.weights(1.0), resultKey);
        sync.expire(searchKey, DEFAULT_EXPIRE_SECONDS);
        sync.del(resultKey);
      }
      else if (hasGeoSet) {
        totalSearchResultsFuture = sync.zinterstore(searchKey, geoKey);
        sync.expire(searchKey, DEFAULT_EXPIRE_SECONDS);
        sync.del(geoKey);
      }


      List<String> facilityIdentifiers = sync
          .zrange(searchKey, page.offset(), page.offset() + page.size());

      this.facilityDao.fetchBatchAsync(facilityIdentifiers);
    }



    return null;
  }
}
