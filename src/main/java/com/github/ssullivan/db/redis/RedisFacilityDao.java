package com.github.ssullivan.db.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IServiceCodesDao;
import com.github.ssullivan.model.AvailableServices;
import com.github.ssullivan.model.Category;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.FacilityWithRadius;
import com.github.ssullivan.model.GeoPoint;
import com.github.ssullivan.model.GeoRadiusCondition;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchRequest;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.ServicesCondition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.spotify.futures.CompletableFutures;
import io.lettuce.core.GeoArgs.Unit;
import io.lettuce.core.GeoRadiusStoreArgs;
import io.lettuce.core.KeyValue;
import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.ZStoreArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisFacilityDao implements IFacilityDao {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisFacilityDao.class);
  private static final long DEFAULT_TIMEOUT = 1500;
  private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

  private static final String SEARCH_REQ = "search:counter";
  private static final String SEARCH_BY_SERVICE_REQ = "search:services:counter";

  private static final String KEY = "treatment:facilities";
  private static final String PK_KEY = "treatment:facilities:counter";
  private static final String INDEX_BY_SERVICES = "index:facility_by_service";
  private static final String INDEX_BY_CATEGORIES = "index:facility_by_category";
  private static final String INDEX_BY_GEO = "index:facility_by_geo";
  public static final int DEFAULT_EXPIRE_SECONDS = 3600;


  private IRedisConnectionPool redis;
  private IAsyncRedisConnectionPool asyncPool;
  private ICategoryCodesDao categoryCodesDao;
  private IServiceCodesDao serviceCodesDao;
  private IFeedDao feedDao;
  private ObjectMapper objectMapper;
  private ObjectReader objectReader;
  private ObjectWriter objectWriter;
  private RollingIdGenerator searchIdGenerator;

  @Inject
  public RedisFacilityDao(IRedisConnectionPool connectionPool,
      IAsyncRedisConnectionPool asyncConnectionPool,
      ICategoryCodesDao categoryCodesDao,
      IServiceCodesDao serviceCodesDao,
      RollingIdGenerator searchIdGenerator,
      IFeedDao feedDao,
      ObjectMapper objectMapper) {
    this.searchIdGenerator = searchIdGenerator;
    this.asyncPool = asyncConnectionPool;
    this.categoryCodesDao = categoryCodesDao;
    this.serviceCodesDao = serviceCodesDao;
    this.redis = connectionPool;
    this.feedDao = feedDao;
    this.objectMapper = objectMapper;
    this.objectReader = objectMapper.readerFor(Facility.class);
    this.objectWriter = objectMapper.writerFor(Facility.class);
  }

  private String facilityKey(final String feed) {
    if (feed != null && !feed.isEmpty()) {
      return KEY + ":" + feed;
    }
    return KEY;
  }

  private long generatePrimaryKey() throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      return generatePrimaryKey(connection.sync());
    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }

  private long generatePrimaryKey(RedisCommands<String, String> redis) throws IOException {
    try {
      return redis.incr(PK_KEY);
    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }

  private List<Long> generatePrimaryKeys(RedisCommands<String, String> redis, final int numKeys) throws IOException {
    final ImmutableList.Builder<Long> builder = new Builder<>();
    for (int i = 1; i <= numKeys; ++i) {
      builder.add(redis.incr(PK_KEY));
    }
    return builder.build();
  }

  private CompletionStage<List<Long>> generatePrimaryKeysAysnc(RedisAsyncCommands<String, String> redis, final int numKeys) {
    final ImmutableList.Builder<CompletionStage<Long>> builder = new Builder<>();
    for (int i = 1; i <= numKeys; ++i) {
      builder.add(redis.incr(PK_KEY));
    }
    redis.getStatefulConnection().flushCommands();
    return CompletableFutures.allAsList(builder.build());
  }

  public List<Facility> list(final Page page) {
    return new ArrayList<>();
  }

  @Override
  public void addFacility(String feed, Facility facility) throws IOException {
    if (facility.getId() <= 0) {
      final long pk = generatePrimaryKey();
      facility.setId(pk);
    }

    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {


      addFacility(connection.sync(), feed, facility);

      connection.setAutoFlushCommands(true);
      connection.flushCommands();
    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        LOGGER.error("Interrupted while add facility", e);
        Thread.currentThread().interrupt();
      }
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }

  private void addFacility(final RedisCommands<String, String> redis, String feed, Facility facility) throws IOException {
    if (facility.getId() <= 0) {
      final long pk = generatePrimaryKey();
      facility.setId(pk);
    }

      final Map<String, String> stringStringMap = toStringMap(facility);
      redis.hmset(KEY + ":" + facility.getId(), stringStringMap);
      // connection.sync().expire(KEY + ":" + facility.getId(), 86400 * 180);

      indexFacility(redis, feed, facility);
  }


  @Override
  public CompletableFuture<List<Boolean>> addFacilitiesAsync(final String feed, final Collection<Facility> batch) throws IOException {
    StatefulRedisConnection<String, String> connection = null;
    try {
      connection = this.redis.borrowConnection();

      final RedisCommands<String, String> sync = connection.sync();

      List<Long> primaryKeys = null;

      try {
        connection.setAutoFlushCommands(false);
        primaryKeys =
            generatePrimaryKeysAysnc(connection.async(), batch.size())
            .toCompletableFuture()
            .get(30, TimeUnit.SECONDS);
        connection.flushCommands();
      }
      finally {
        connection.setAutoFlushCommands(true);
      }

      if (null == primaryKeys || primaryKeys.size() != batch.size()) {
        connection.close();
        throw new IOException("Failed to generate primary keys for batch!");
      }


      int i = 0;
      List<CompletableFuture<Boolean>> promises = new ArrayList<>();
      try {
        connection.setAutoFlushCommands(false);
        for (final Facility facility : batch) {
          facility.setId(primaryKeys.get(i));
          final Map<String, String> stringStringMap = toStringMap(facility);
          final CompletableFuture<String> storeFacilityPromise = connection.async().hmset(KEY + ":" + facility.getId(), stringStringMap).toCompletableFuture();


          final CompletableFuture<Boolean> indexPromise = indexFacilityAsync(connection.async(), feed, facility).toCompletableFuture();

          final CompletionStage<Boolean> combined = CompletableFutures.combineFutures(storeFacilityPromise, indexPromise, (firstResult, secondResult) -> CompletableFuture.completedFuture(true));
          promises.add(combined.toCompletableFuture());

          connection.flushCommands();
          i = i + 1;
        }
      }
      finally {
        connection.setAutoFlushCommands(true);
      }

      final StatefulRedisConnection<String, String> redis = connection;
      return CompletableFutures.allAsList(promises)
          .whenComplete((result, error) -> {
            redis.setAutoFlushCommands(true);
            redis.close();
          });
    } catch (Exception e) {
      if (connection != null) {
        connection.close();
      }
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }

      throw new IOException("Failed to get connection to REDIS", e);
    }
  }

  public void addFacility(final Facility facility) throws IOException {
    addFacility(this.feedDao.currentFeedId().orElse(""), facility);
  }

  public Facility getFacility(final String pk) throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      return getFacility(connection, pk);
    } catch (Exception e) {
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }




  @Override
  public CompletionStage<SearchResults<Facility>> find(SearchRequest searchRequest,
      Page page) throws Exception {

    if (null == searchRequest) {
      LOGGER.warn("Invalid Search Request of null. No Results!");
      return CompletableFuture.completedFuture(SearchResults.searchResults(0));
    }

    final Optional<String> feedOption = this.feedDao.currentFeedId();

    if (!feedOption.isPresent()) {
      LOGGER.error("Error connecting to the backend database! Unable to find current feed");
      return CompletableFuture.completedFuture(SearchResults.empty());
    }

    final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection();
    final String searchKey = "s:" + this.searchIdGenerator.generateId(SEARCH_REQ) + ":" + System.currentTimeMillis() + ":";
    final String resultKey = searchKey + ":0";
    final String geoKey = searchKey + ":geo";

    final RedisAsyncCommands<String, String> redis = connection.async();
    redis.setAutoFlushCommands(false);


    // Perform the following operations in Batch
    boolean hasResultKey = false;
    final List<ServicesCondition> servicesConditions = searchRequest.getConditions();

    if (!servicesConditions.isEmpty()) {
      int counter = 0;

      final String[] keys = new String[servicesConditions.size()];
      for (final ServicesCondition servicesCondition : servicesConditions) {
        final String key = searchKey + "s:" + counter;
        if (!createServiceSearchSet(redis, key, feedOption.orElse(""), servicesCondition)) {
          LOGGER.warn("Failed to create search set: {}", key);
        }
        keys[counter] = key;
        counter = counter + 1;
      }

      switch (searchRequest.getFinalSetOperation()) {
        case INTERSECTION:
          redis.sinterstore(resultKey, keys);
        case UNION:
          redis.sunionstore(resultKey, keys);
        default:
          redis.sinterstore(resultKey, keys);
      }

      redis.del(keys);
      hasResultKey = true;
    }

    final boolean hasGeoSet = createGeoSearchSet(redis,
        geoKey,
        feedOption.orElse(""),
        searchRequest.getGeoRadiusCondition());

    CompletionStage<Long> totalSearchResultsFuture = CompletableFuture.completedFuture(0L);
    if (hasResultKey && hasGeoSet) {

      redis.zinterstore(searchKey, ZStoreArgs.Builder.weights(1.0), resultKey);

      // #3 Find the intersection of the places that have our services we want and
      //    are within a specific radius
      totalSearchResultsFuture = redis.zinterstore(searchKey, searchKey, geoKey);
      redis.expire(searchKey, DEFAULT_EXPIRE_SECONDS);
      redis.del(resultKey, geoKey);
    }
    else if (hasResultKey) {
      totalSearchResultsFuture = redis.zinterstore(searchKey, ZStoreArgs.Builder.weights(1.0), resultKey);
      redis.expire(searchKey, DEFAULT_EXPIRE_SECONDS);
      redis.del(resultKey);
    }
    else if (hasGeoSet) {
      totalSearchResultsFuture = redis.zinterstore(searchKey, geoKey);
      redis.expire(searchKey, DEFAULT_EXPIRE_SECONDS);
      redis.del(geoKey);
    }

    redis.flushCommands();
    redis.setAutoFlushCommands(true);


    // #4 Fetch the results
  final CompletionStage<List<String>> idFutures = connection.async()
      .zrange(searchKey, page.offset(), page.offset() + page.size());
    redis.getStatefulConnection().setAutoFlushCommands(true);
  return CompletableFutures.combineFutures(idFutures, totalSearchResultsFuture, (ids, totalResults) -> fetchBatchAsync(redis, ids)
      .thenApply(facilities -> facilities
          .stream()
          .peek(facility -> {
            final AvailableServices availableServices = getAvailableServices(facility);
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

              return toFacilityWithRadius(facility, latitude, longitude, geoUnit);
            }

            return facility;
          })
          .collect(Collectors.toList()))
      .thenApply(facilities -> SearchResults.searchResults(totalResults, facilities)))
      .whenComplete((result, error) -> {
        if (error != null) {
          LOGGER.error("An error occurred while processing search", error);
        }
        // #6 Explicitly delete the keys
        redis.del(searchKey);
        connection.setAutoFlushCommands(true);
        connection.close();
      });
  }

  private FacilityWithRadius toFacilityWithRadius(Facility facility, double latitude, double longitude,
      String geoUnit) {
    if (facility.getLocation() != null) {

      return new FacilityWithRadius(facility, facility.getLocation()
          .getDistance(GeoPoint.geoPoint(latitude, longitude), geoUnit),
          geoUnit);
    }

    return new FacilityWithRadius(facility, 0.0, geoUnit);
  }

  private boolean createServiceSearchSet(final RedisAsyncCommands<String, String> redis,
      final String key,
      final String feed,
      final ServicesCondition condition) {
    if (condition != null && !condition.getServices().isEmpty()) {
      // We have a key per service code in redis
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

  private boolean createGeoSearchSet(final RedisAsyncCommands<String, String> redis,
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


  private Facility getFacility(final StatefulRedisConnection<String, String> connection,
      final String pk) {
      return toFacility(connection.sync().hmget(KEY + ":" + pk, "_source"));
  }

  private CompletionStage<Facility> getFacilityAsync(
      final RedisAsyncCommands<String, String> asyncCommands,
      final String pk) {

    return asyncCommands.hmget(KEY + ":" + pk, "_source")
        .thenApply(this::toFacility);
  }

  private Facility toFacility(final List<KeyValue<String, String>> fields) {
    if (fields != null && !fields.isEmpty()) {
      return deserialize(fields.get(0).getValue(), null);
    }
    return null;
  }

  private String[] getServiceCodeIndices(final String feed, final Collection<String> serviceCodes) {
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

  private String[] getServiceCodeIndices(final Collection<String> serviceCodes) {
    return getServiceCodeIndices("", serviceCodes);
  }

  @Override
  public SearchResults<Facility> findByServiceCodes(Collection<String> serviceCodes, Page page)
      throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {

      final String searchKey = "s:" + this.searchIdGenerator.generateId(SEARCH_REQ);

      final String[] serviceCodeSets = getServiceCodeIndices(serviceCodes);

      final Long numResults = connection.sync().zinterstore(searchKey, serviceCodeSets);

      final List<String> ids = connection.sync()
          .zrange(searchKey, page.offset(), page.offset() + page.size());

      final List<Facility> searchResults = fetchBatch(connection.async(), ids);

      connection.sync().del(searchKey);

      return SearchResults.searchResults(numResults, searchResults);
    } catch (Exception e) {
      LOGGER.error("Failed to find any facilities with serviceCodes: {}, page: {}", serviceCodes,
          page);
      throw new IOException("Failed to find any matching results", e);
    }
  }

  private Set<String> createSearchKey(final StatefulRedisConnection<String, String> connection,
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

  private static boolean isEmpty(final Collection<String> list) {
    return list == null || list.isEmpty();
  }

  @Override
  @Deprecated
  public SearchResults<Facility> findByServiceCodes(final Collection<String> serviceCodes, final Collection<String> mustNotServiceCodes,
      final boolean matchAny,  final Page page)
      throws IOException {

    if (isEmpty(serviceCodes) && isEmpty(mustNotServiceCodes)) {
      return SearchResults.searchResults(0, ImmutableList.of());
    }

    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {

      final String searchKey = "s:" + this.searchIdGenerator.generateId(SEARCH_REQ);


      long numResults = 0;

      final Set<String> keysToDelete = createSearchKey(connection, searchKey, serviceCodes,
          mustNotServiceCodes, matchAny);

      final List<String> ids = connection.sync()
          .zrange(searchKey, page.offset(), page.offset() + page.size());

      final List<Facility> searchResults = fetchBatch(connection.async(), ids);

      try {
        keysToDelete.forEach(it -> connection.async().del(it));
        connection.async().del(searchKey);
        connection.flushCommands();
      }
      finally {
        connection.sync().getStatefulConnection().setAutoFlushCommands(true);
      }

      return SearchResults.searchResults(numResults, searchResults);
    } catch (Exception e) {
      LOGGER.error("Failed to find any facilities with serviceCodes: {}, page: {}", serviceCodes,
          page);
      throw new IOException("Failed to find any matching results", e);
    }
  }

  private CompletionStage<List<Facility>> fetchBatchAsync(final RedisAsyncCommands<String, String> asyncCommands, final Collection<String> ids) {
    if (asyncCommands == null || ids == null) {
     return CompletableFuture.completedFuture(new ArrayList<>(0));
    }

    final Set<String> distinctIdentifiers = new HashSet<>(ids);
    final List<CompletionStage<Facility>> facilityFutures =
        distinctIdentifiers.stream()
        .map(id -> getFacilityAsync(asyncCommands, id))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

   return CompletableFutures.successfulAsList(facilityFutures, t -> {
      LOGGER.error("Fetching one of the facilities failed", t);
      return null;
    }).thenApply(
        results -> results.stream().filter(Objects::nonNull).collect(Collectors.toList()));
  }

  private List<Facility> fetchBatch(final RedisAsyncCommands<String, String> asyncCommands, final List<String> ids) {
    if (asyncCommands == null || ids == null) {
      return new ArrayList<>(0);
    }


    // #5 Fetch each facility
    final List<CompletionStage<Facility>> facilityFutures =
        ids.stream().map(id -> getFacilityAsync(asyncCommands, id))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    final CompletableFuture<List<Facility>>
        batchFuture = CompletableFutures.successfulAsList(facilityFutures, t -> {
      LOGGER.error("Fetching one of the facilities failed", t);
      return null;
    }).thenApply(
        results -> results.stream().filter(Objects::nonNull).collect(Collectors.toList()));

    asyncCommands.flushCommands();

    if (!LettuceFutures.awaitAll(Duration.ofSeconds(DEFAULT_TIMEOUT), batchFuture)) {
      return new ArrayList<>(0);
    }

    return batchFuture.getNow(new ArrayList<>(0));


  }

  @Override
  public SearchResults<FacilityWithRadius> findByServiceCodesWithin(final Collection<String> mustServiceCodes,
      final Collection<String> mustNotServiceCodes,
      final boolean matchAny,
      final double longitude,
      final double latitude,
      final double distance,
      final String geoUnit,
      final Page page) throws IOException {

    int j = 0;
    final String[] uniqMust = getServiceCodeIndices(new HashSet<>(mustServiceCodes));
    final String[] uniqMustNot = getServiceCodeIndices(new HashSet<>(mustNotServiceCodes));

    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {

      final String searchId = this.searchIdGenerator.generateId(SEARCH_REQ) + "";
      final String searchKey = "s:" + searchId;
      final String searchKeyMust = searchKey + ":m";
      final String searchKeyMustNot = searchKey + ":n";
      final String searchKeyDiff = searchKey + ":d";
      final String radiusKey = "s:geo:" + searchId;
      final String searchKeyFinal = searchKey + ":1";



      final RedisAsyncCommands<String, String> asyncCommands = connection.async();
      asyncCommands.setAutoFlushCommands(false);
      asyncCommands.multi();

      // TODO: I think we can do a lua script here to reduce round trips
      // #1 Find all of the centers within a certain radius

      asyncCommands
          .georadius(INDEX_BY_GEO, longitude, latitude, distance, Unit.valueOf(geoUnit),
              GeoRadiusStoreArgs.Builder
                  .withStoreDist(radiusKey));

      /**
       * Find the places that have services we want
       * Find the places that have services we don't want
       */
      if (matchAny && uniqMust.length > 0) {
        asyncCommands.sunionstore(searchKeyMust, uniqMust);
      }
      else if (!matchAny && uniqMust.length > 0) {
        asyncCommands.sinterstore(searchKeyMust, uniqMust);
      }
      if (uniqMustNot.length > 0)
        asyncCommands.sinterstore(searchKeyMustNot, uniqMustNot);

      asyncCommands.sdiffstore(searchKeyDiff, searchKeyMust, searchKeyMustNot);
      asyncCommands.zinterstore(searchKey, ZStoreArgs.Builder.weights(1.0), searchKeyDiff);
      asyncCommands.del(searchKeyMust, searchKeyMust, searchKeyDiff);


      // #3 Find the intersection of the places that have our services we want and
      //    are within a specific radius
      RedisFuture<Long> geoServicesIntersectionFuture = asyncCommands
          .zinterstore(searchKeyFinal, searchKey, radiusKey);



      final RedisFuture<TransactionResult> multiFuture = asyncCommands.exec();
      asyncCommands.flushCommands();
      asyncCommands.setAutoFlushCommands(true);

      try {
        TransactionResult transactionResult = multiFuture.get(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT);

        // #4 Fetch the results
        final List<String> ids = connection.sync()
            .zrange(searchKeyFinal, page.offset(), page.offset() + page.size());


        return getFacilityWithRadiusSearchResults(longitude, latitude, geoUnit, asyncCommands,
            geoServicesIntersectionFuture, ids);

      } catch (InterruptedException e) {
        LOGGER.error("Interrupted while waiting for multi result", e);
        Thread.currentThread().interrupt();
      } catch (TimeoutException e) {
        LOGGER.error("Timeout while waiting for multi result", e);
      }
      finally {
        // #6 Explicitly delete the keys
        connection.async().del(searchKey, searchKeyFinal, radiusKey);
      }
    }
    catch (Exception e) {
      LOGGER.error("Failed to find any facilities with serviceCodes: {}, page: {}", "-",
          page);
      throw new IOException("Failed to find any matching results", e);
    }
    return SearchResults.searchResults(0L);
  }


  private SearchResults<FacilityWithRadius> getFacilityWithRadiusSearchResults(final double longitude,
      final double latitude, final String geoUnit, final RedisAsyncCommands<String, String> asyncCommands,
      final CompletionStage<Long> geoServicesIntersectionFuture, final List<String> ids) {

    final List<FacilityWithRadius> searchResults =
        fetchBatch(asyncCommands, ids)
            .stream()
            .map(facility -> {
              final AvailableServices availableServices = getAvailableServices(facility);
              facility.setAvailableServices(availableServices);

              return toFacilityWithRadius(facility, latitude, longitude, geoUnit);
            })
           .collect(Collectors.toList());

    return SearchResults.searchResults(geoServicesIntersectionFuture.toCompletableFuture().getNow(0L), searchResults);
  }


  @Override
  @Deprecated
  public SearchResults<FacilityWithRadius> findByServiceCodesWithin(List<String> serviceCodes,
      double longitude, double latitude, double distance, String geoUnit, Page page)
      throws IOException {

    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {

      final String searchId = this.searchIdGenerator.generateId(SEARCH_REQ) + "";
      final String searchKey = "s:" + searchId;
      final String radiusKey = "s:geo:" + searchId;


      final String[] serviceCodeSets = getServiceCodeIndices(serviceCodes);

      final RedisAsyncCommands<String, String> asyncCommands = connection.async();
      asyncCommands.multi();

      // TODO: I think we can do a lua script here to reduce round trips
      // #1 Find all of the centers within a certain radius

      RedisFuture<Long> indexGeoSearchFuture = asyncCommands
          .georadius(INDEX_BY_GEO, longitude, latitude, distance, Unit.valueOf(geoUnit),
              GeoRadiusStoreArgs.Builder
                  .withStoreDist(radiusKey));





      // #2 Find all of the centers with the specified services
      RedisFuture<Long> serviceUnionFuture = asyncCommands.zinterstore(searchKey, serviceCodeSets);

      // #3 Find the intersection of the places that have our services we want and
      //    are within a specific radius
      RedisFuture<Long> geoServicesIntersectionFuture = asyncCommands
          .zinterstore(searchKey + ":" + 1, searchKey, radiusKey);

      final RedisFuture<TransactionResult> multiFuture = asyncCommands.exec();
      try {
        TransactionResult transactionResult = multiFuture.get(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT);

        // #4 Fetch the results
        final List<String> ids = connection.sync()
            .zrange(searchKey + ":" + 1, page.offset(), page.offset() + page.size());

        return getFacilityWithRadiusSearchResults(longitude, latitude, geoUnit, asyncCommands,
            geoServicesIntersectionFuture, ids);
      } catch (InterruptedException e) {
        LOGGER.error("Interrupted while waiting for multi result", e);
        Thread.currentThread().interrupt();
      } catch (TimeoutException e) {
        LOGGER.error("Timeout while waiting for multi result", e);
      }
      finally {
        // #6 Explicitly delete the keys
        connection.async().del(searchKey, searchKey + ":1", radiusKey);
      }
    }
    catch (Exception e) {
      LOGGER.error("Failed to find any facilities with serviceCodes: {}, page: {}", serviceCodes,
          page);
      throw new IOException("Failed to find any matching results", e);
    }
    return SearchResults.searchResults(0L);
  }

  @Deprecated
  public void indexFacility(final RedisCommands<String, String> sync, final Facility facility)
      throws IOException {
    LOGGER.warn("This function will be deprecated in the future!");
    indexFacility(sync, "", facility);
  }

  private void indexFacility(final RedisCommands<String, String> sync, final String feed, final Facility facility)
      throws IOException {
    indexFacilityByGeo(sync, feed, facility);
    indexFacilityByServiceCodes(sync, feed, facility);
    indexFacilityByCategoryCodes(sync, feed, facility);
  }

  private CompletionStage<Boolean> indexFacilityAsync(final RedisAsyncCommands<String, String> async, final String feed, final Facility facility)
      throws IOException {
    CompletableFuture<Long> geoPromise = indexFacilityByGeoAsync(async, feed, facility);
    CompletableFuture<List<Long>> servicesPromise = indexFacilityByServiceCodesAsync(async, feed, facility);
    CompletableFuture<List<Long>> catsPromise = indexFacilityByCategoryCodesAsync(async, feed, facility);

    return CompletableFutures.combineFutures(geoPromise, servicesPromise, catsPromise, (geoResult, servicesResult, catsResult)-> {
      LOGGER.debug("Finished indexing facility");
      return CompletableFuture.completedFuture(true);
    });
  }

  private CompletableFuture<Long> indexFacilityByGeoAsync(final RedisAsyncCommands<String, String> async, final String feed, final Facility facility) {
    if (facility == null) {
      throw new NullPointerException("Facility must not be null");
    }

    if (facility.getId() == 0) {
      throw new IllegalArgumentException("id must be non zero");
    }

    if (facility.getLocation() == null) {
      throw new NullPointerException("Location must not be null");
    }

    return async
        .geoadd(indexByGeoKey(feed), facility.getLocation().lon(), facility.getLocation().lat(),
            Long.toString(facility.getId(), 10))
        .toCompletableFuture();
  }

  private void indexFacilityByGeo(final RedisCommands<String, String> sync, final String feed, final Facility facility) {
    if (facility == null) {
      return;
    }

    if (facility.getId() == 0) {
      throw new IllegalArgumentException("id must be non zero");
    }

    if (facility.getLocation() == null) {
      return;
    }



    final Long geoAddCount = sync
        .geoadd(indexByGeoKey(feed), facility.getLocation().lon(), facility.getLocation().lat(),
            Long.toString(facility.getId(), 10));

    LOGGER.debug("{} items added to set {}", geoAddCount, indexByGeoKey(feed));
  }

  private String indexByGeoKey(final String feed) {
    String key = INDEX_BY_GEO;
    if (feed != null && !feed.isEmpty()) {
      key = key + ":" + feed;
    }
    return key;
  }

  private CompletableFuture<List<Long>> indexFacilityByCategoryCodesAsync(final RedisAsyncCommands<String, String> async,
      final String feed, final Facility facility) {
    if (facility == null) {
      throw new NullPointerException("facility must not be null");
    }

    if (facility.getId() == 0) {
      throw new IllegalArgumentException("id must be non zero");
    }

    final List<CompletableFuture<Long>> promises  = facility.getCategoryCodes()
        .stream()
        .filter(Objects::nonNull)
        .filter(it -> !it.isEmpty())
        .map(code -> {
          String key = INDEX_BY_CATEGORIES + ":" + code;
          if (feed != null && !feed.isEmpty()) {
            key = INDEX_BY_CATEGORIES + ":" + feed + ":" + code;
          }
          return async.sadd(key, Long.toString(facility.getId(), 10))
              .toCompletableFuture();
        })
        .collect(Collectors.toList());
    async.getStatefulConnection().flushCommands();
    return CompletableFutures.successfulAsList(promises, e -> {
      LOGGER.error("", e);
      return 0L;
    });

  }

  private void indexFacilityByCategoryCodes(final RedisCommands<String, String> sync,
      final String feed, final Facility facility) {
    if (facility == null) {
      return;
    }

    if (facility.getId() == 0) {
      throw new IllegalArgumentException("id must be non zero");
    }

    facility.getCategoryCodes()
        .stream()
        .filter(Objects::nonNull)
        .filter(it -> !it.isEmpty())
        .forEach(code -> {
          String key = INDEX_BY_CATEGORIES + ":" + code;
          if (feed != null && !feed.isEmpty()) {
            key = INDEX_BY_CATEGORIES + ":" + feed + ":" + code;
          }
          sync.sadd(key, Long.toString(facility.getId(), 10));
        });
  }


  private CompletableFuture<List<Long>> indexFacilityByServiceCodesAsync(final RedisAsyncCommands<String, String> async,
      final String feed,
      final Facility facility) throws IOException {
    if (facility == null) {
      throw new NullPointerException("Facility must not be null");
    }

    if (facility.getId() == 0) {
      throw new IllegalArgumentException("id must be non zero");
    }

    final List<CompletableFuture<Long>> promises  = facility.getServiceCodes()
        .stream()
        .filter(Objects::nonNull)
        .filter(it -> !it.isEmpty())
        .map(code -> {
          String key = INDEX_BY_SERVICES + ":" + code;
          if (feed != null && !feed.isEmpty()) {
            key = INDEX_BY_SERVICES + ":" + feed + ":" + code;
          }
          return async.sadd(key, Long.toString(facility.getId(), 10)).toCompletableFuture();
        })
        .collect(Collectors.toList());

    async.getStatefulConnection().flushCommands();

    return CompletableFutures.successfulAsList(promises, e -> {
      LOGGER.error("", e);
      return 0L;
    });

  }

  private void indexFacilityByServiceCodes(final RedisCommands<String, String> sync,
      final String feed,
      final Facility facility) throws IOException {
    if (facility == null) {
      return;
    }

    if (facility.getId() == 0) {
      throw new IllegalArgumentException("id must be non zero");
    }

    facility.getServiceCodes()
        .stream()
        .filter(Objects::nonNull)
        .filter(it -> !it.isEmpty())
        .forEach(code -> {
          String key = INDEX_BY_SERVICES + ":" + code;
          if (feed != null && !feed.isEmpty()) {
            key = INDEX_BY_SERVICES + ":" + feed + ":" + code;
          }
          sync.sadd(key, Long.toString(facility.getId(), 10));
        });
  }

  @Override
  public SearchResults findByServiceCodes(ImmutableSet<String> mustServiceCodes,
      Page page) throws IOException {
    return findByServiceCodes(ImmutableList.copyOf(mustServiceCodes), page);
  }

  private Map<String, String> toStringMap(final Facility facility) throws JsonProcessingException {
    final Map<String, String> toReturn = new HashMap<>();
    toReturn.put("id", "" + facility.getId());
    toReturn.put("_source", objectWriter.writeValueAsString(facility));
    toReturn.put("name1", facility.getName1());
    toReturn.put("name2", facility.getName2());
    toReturn.put("zip", facility.getZip());
    toReturn.put("street", facility.getStreet());
    toReturn.put("city", facility.getCity());
    toReturn.put("state", facility.getState());
    toReturn.put("county", facility.getCounty());
    toReturn.put("googlePlaceId", facility.getGooglePlaceId());
    toReturn.put("formattedAddress", facility.getFormattedAddress());
    toReturn.put("website", facility.getWebsite());
    toReturn.put("phoneNumbers", objectMapper.writeValueAsString(facility.getPhoneNumbers()));
    facility.getCategoryCodes()
        .forEach(code -> toReturn.put("c:" + code, "1"));
    facility.getServiceCodes()
        .forEach(code -> toReturn.put("s:" + code, "1"));

    if (facility.getLocation() != null) {
      toReturn.put("location.lat", "" + facility.getLocation().lat());
      toReturn.put("location.lon", "" + facility.getLocation().lon());
    }

    return toReturn;
  }

  private String serialize(@Nonnull final Facility category) throws IOException {
    return objectWriter.writeValueAsString(category);
  }

  private Facility deserialize(@Nonnull final String json) throws IOException {
    return objectReader.readValue(json);
  }

  private Facility deserialize(@Nonnull final String json, final Facility defaultValue) {
    if (json.isEmpty()) {
      return defaultValue;
    }
    try {
      return deserialize(json);
    } catch (IOException e) {
      LOGGER.error("Failed to deserialize JSON for category", e);
      return defaultValue;
    }
  }

  private AvailableServices getAvailableServices(final Facility facility) {
    if (facility == null) {
      return new AvailableServices();
    }

    final Map<String, Category> availableServicesByCategory = new HashMap<>();

    for (final String categoryCode : facility.getCategoryCodes()) {
      final Category availableCategory =
          availableServicesByCategory.getOrDefault(categoryCode, new Category(categoryCode, "",  new HashSet<String>()));
      availableServicesByCategory.putIfAbsent(categoryCode, availableCategory);


      final Category category = this.categoryCodesDao.getFromCache(categoryCode);
      if (category != null) {
        availableCategory.setName(category.getName());
        availableCategory.setCode(category.getCode());

        category.getServiceCodes()
            .stream()
            .filter(service -> facility.getServiceCodes().contains(service))
            .map(service -> {
              try {
                return serviceCodesDao.getFromCache(service);
              } catch (IOException e) {
                LOGGER.error("Failed to get service information for '{}'", service, e);
              }
              return null;
            })
            .filter(Objects::nonNull)
            .forEach(availableCategory::addServiceCode);
      }

    }

    return new AvailableServices(availableServicesByCategory.values());
  }
}
