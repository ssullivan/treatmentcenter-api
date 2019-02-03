package com.github.ssullivan.db.redis.search;

import static com.github.ssullivan.db.redis.RedisConstants.DEFAULT_EXPIRE_SECONDS;
import static com.github.ssullivan.db.redis.RedisConstants.INDEX_BY_GEO;
import static com.github.ssullivan.db.redis.RedisConstants.INDEX_BY_SERVICES;
import static com.github.ssullivan.db.redis.RedisConstants.indexByGeoKey;

import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.redis.IAsyncRedisConnectionPool;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.db.redis.ToFacilityWithRadiusConverter;
import com.github.ssullivan.model.AvailableServices;
import com.github.ssullivan.model.FacilityWithRadius;
import com.github.ssullivan.model.GeoRadiusCondition;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.ServicesCondition;
import io.lettuce.core.GeoRadiusStoreArgs;
import io.lettuce.core.ZStoreArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.RedisCommand;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFindFacility {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFindFacility.class);

  protected IRedisConnectionPool syncPool;
  protected IFacilityDao facilityDao;
  protected IFeedDao feedDao;
  protected IAsyncRedisConnectionPool asyncPool;
  public AbstractFindFacility(IRedisConnectionPool redis,
      IAsyncRedisConnectionPool asyncPool,
      IFacilityDao facilityDao,
      IFeedDao feedDao) {
    this.syncPool = redis;
    this.facilityDao = facilityDao;
    this.feedDao = feedDao;
    this.asyncPool = asyncPool;
  }

  protected static Set<String> createSearchKey(final StatefulRedisConnection<String, String> connection,
      final String searchKey, final Collection<String> serviceCodes, final Collection<String> mustNotServiceCodes,
      final boolean matchAny) {


    final String searchKeyMust = searchKey + ":m";
    final String searchKeyMustNot = searchKey + ":n";
    final String searchKeyDiff = searchKey + ":d";

    Set<String> retval = new HashSet<>();
    retval.add(searchKey);
    retval.add(searchKeyMust);
    retval.add(searchKeyMustNot);
    retval.add(searchKeyDiff);
    try {
      connection.setAutoFlushCommands(false);
      RedisAsyncCommands<String, String> async = connection.async();



      final String[] uniqMust = getServiceCodeIndices(new HashSet<>(serviceCodes));
      final String[] uniqMustNot = getServiceCodeIndices(new HashSet<>(mustNotServiceCodes));


      if (uniqMust.length <= 0 && uniqMustNot.length <= 0) {
        return new HashSet<>();
      }

      if (uniqMust.length > 0 && uniqMustNot.length <= 0) {
        if (matchAny) {
          async.zunionstore(searchKey, uniqMust);
        } else {
          async.zinterstore(searchKey, uniqMust);
        }
      } else if (uniqMust.length > 0 && uniqMustNot.length > 0) {
        if (matchAny) {
          async.sunionstore(searchKeyMust, uniqMust);
        } else {
          async.sinterstore(searchKeyMust, uniqMust);
        }


        async.sinterstore(searchKeyMustNot, uniqMustNot);
        async.sdiffstore(searchKeyDiff, searchKeyMust, searchKeyMustNot);
        async.zinterstore(searchKey, ZStoreArgs.Builder.weights(1.0), searchKeyDiff);
        async.flushCommands();

      }
    }
    finally {
      connection.flushCommands();
      connection.setAutoFlushCommands(true);
    }

    return retval;
  }

  protected SearchResults<FacilityWithRadius> getFacilityWithRadiusSearchResults(final double longitude,
      final double latitude, final String geoUnit,
      final CompletionStage<Long> geoServicesIntersectionFuture, final List<String> ids) {

    final ToFacilityWithRadiusConverter converter = new ToFacilityWithRadiusConverter(latitude, longitude, geoUnit);

    final List<FacilityWithRadius> searchResults =
        facilityDao.fetchBatch(ids)
            .stream()
            .map(facility -> {
              final AvailableServices availableServices = facilityDao.getAvailableServices(facility);
              facility.setAvailableServices(availableServices);

              return converter.apply(facility);
            })
            .collect(Collectors.toList());

    return SearchResults.searchResults(geoServicesIntersectionFuture.toCompletableFuture().getNow(0L), searchResults);
  }

  protected static boolean createServiceSearchSet(final RedisCommands<String, String> redis,
      final String key,
      final String feed,
      final ServicesCondition condition) {
    if (condition != null && !condition.getServices().isEmpty()) {
      // We have a key per service code in syncPool
      // Here we are constructing all of these keys based on the services
      // the first condition specified
      final String[] serviceKeys = getServiceCodeIndices(condition.getServices());
      switch (condition.getMatchOperator()) {
        case MUST:
          redis.sinterstore(key, serviceKeys);
          break;
        case SHOULD:
          redis.sunionstore(key, serviceKeys);
          break;
        default:
          redis.sdiffstore(key, serviceKeys);
          break;
      }

      if (condition.getMustNotServiceCodes() != null && !condition.getMustNotServiceCodes().isEmpty()) {
        // This is an optimization - We don't need to calc the diff against
        // all service keys. We can just do the diff against the ones that matched
        // above that are stored in the resultKey
        final String mustNotKey = key + ":n";

        redis.sadd(mustNotKey, condition.getMustNotServiceCodes().toArray(new String[]{}));
        redis.sdiffstore(key, key, mustNotKey);
        redis.expire(mustNotKey, DEFAULT_EXPIRE_SECONDS);
        redis.del(mustNotKey);
      }

      redis.expire(key, DEFAULT_EXPIRE_SECONDS);
      return true;
    }
    return false;
  }


  protected static boolean createServiceSearchSet(final RedisAsyncCommands<String, String> redis,
      final String key,
      final String feed,
      final ServicesCondition condition) {
    if (condition != null && !condition.getServices().isEmpty()) {
      // We have a key per service code in syncPool
      // Here we are constructing all of these keys based on the services
      // the first condition specified
      final String[] serviceKeys = getServiceCodeIndices(condition.getServices());
      switch (condition.getMatchOperator()) {
        case MUST:
          redis.sinterstore(key, serviceKeys);
          break;
        case SHOULD:
          redis.sunionstore(key, serviceKeys);
          break;
        default:
          redis.sdiffstore(key, serviceKeys);
          break;
      }

      if (condition.getMustNotServiceCodes() != null && !condition.getMustNotServiceCodes().isEmpty()) {
        // This is an optimization - We don't need to calc the diff against
        // all service keys. We can just do the diff against the ones that matched
        // above that are stored in the resultKey
        final String mustNotKey = key + ":n";

        redis.sadd(mustNotKey, condition.getMustNotServiceCodes().toArray(new String[]{}));
        redis.sdiffstore(key, key, mustNotKey);
        redis.expire(mustNotKey, DEFAULT_EXPIRE_SECONDS);
        redis.del(mustNotKey);
      }

      redis.expire(key, DEFAULT_EXPIRE_SECONDS);
      return true;
    }
    return false;
  }

  protected static boolean createGeoSearchSet(final RedisCommands<String, String> redis,
      final String key,
      final String feed, final GeoRadiusCondition condition) {

    if (INDEX_BY_GEO.equalsIgnoreCase(key) || key.startsWith(INDEX_BY_GEO)) {
      LOGGER.warn("Invalid key search key '{}' was specified", key);
      return false;
    }

    if (condition != null && condition.getGeoPoint() != null) {

      redis
          .georadius(indexByGeoKey(feed), condition.getGeoPoint()
                  .lon(), condition.getGeoPoint().lat(),
              condition.getRadius(), condition.getGeoUnit().unit(),
              GeoRadiusStoreArgs.Builder
                  .withStoreDist(key));

      return true;
    }

    return false;
  }

  protected static boolean createGeoSearchSet(final RedisAsyncCommands<String, String> redis,
      final String key,
      final String feed, final GeoRadiusCondition condition) {

    if (INDEX_BY_GEO.equalsIgnoreCase(key) || key.startsWith(INDEX_BY_GEO)) {
      LOGGER.warn("Invalid key search key '{}' was specified", key);
      return false;
    }

    if (condition != null && condition.getGeoPoint() != null) {

      redis
          .georadius(indexByGeoKey(feed), condition.getGeoPoint()
                  .lon(), condition.getGeoPoint().lat(),
              condition.getRadius(), condition.getGeoUnit().unit(),
              GeoRadiusStoreArgs.Builder
                  .withStoreDist(key));

      return true;
    }

    return false;
  }


  protected static String[] getServiceCodeIndices(final Collection<String> serviceCodes) {
    return getServiceCodeIndices("", serviceCodes);
  }

  protected static String[] getServiceCodeIndices(final String feed, final Collection<String> serviceCodes) {
    if (null == serviceCodes || serviceCodes.isEmpty()) return new String[]{};
    return serviceCodes
        .stream()
        .map(code -> {
          if (feed == null || feed.isEmpty()) {
            return INDEX_BY_SERVICES + ":" + code;
          }
          else {
            return INDEX_BY_SERVICES + ":" + feed + ":" + code;
          }
        })
        .collect(Collectors.toSet()).toArray(new String[]{});
  }





}
