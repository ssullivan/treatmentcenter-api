package com.github.ssullivan.db.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.spotify.futures.CompletableFutures;
import io.lettuce.core.GeoArgs.Unit;
import io.lettuce.core.GeoRadiusStoreArgs;
import io.lettuce.core.KeyValue;
import io.lettuce.core.LettuceFutures;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisFacilityDao implements IFacilityDao {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisFacilityDao.class);
  private static final long DEFAULT_TIMEOUT = 15;
  private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

  private static final String SEARCH_REQ = "search:counter";
  private static final String SEARCH_BY_SERVICE_REQ = "search:services:counter";

  private static final String KEY = "treatment:facilities";
  private static final String PK_KEY = "treatment:facilities:counter";
  private static final String INDEX_BY_SERVICES = "index:facility_by_service";
  private static final String INDEX_BY_CATEGORIES = "index:facility_by_category";
  private static final String INDEX_BY_GEO = "index:facility_by_geo";


  private IRedisConnectionPool redis;
  private ObjectMapper objectMapper;
  private ObjectReader objectReader;
  private ObjectWriter objectWriter;

  @Inject
  public RedisFacilityDao(IRedisConnectionPool connectionPool, ObjectMapper objectMapper) {
    this.redis = connectionPool;
    this.objectMapper = objectMapper;
    this.objectReader = objectMapper.readerFor(Facility.class);
    this.objectWriter = objectMapper.writerFor(Facility.class);
  }

  private long generatePrimaryKey() throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      return connection.sync().incr(PK_KEY);
    } catch (Exception e) {
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }

  public List<Facility> list(final Page page) {
    return new ArrayList<>();
  }

  @Override
  public ImmutableMap<Long, Facility> fetchAllFacilities() throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      final ImmutableList<String> redisKeys = ImmutableList.copyOf(connection.sync().keys(KEY + ":*"));

      List<List<String>> partitions = Lists.partition(redisKeys, 100);

      RedisAsyncCommands<String, String> async = connection.async();
      async.setAutoFlushCommands(false);

      final List<CompletionStage<Facility>> futures = new ArrayList<>();

      for (final List<String> part : partitions) {
        for (final String redisKey : part) {
          futures.add(async.hmget(redisKey, "_source")
              .thenApply(this::toFacility));
        }
        async.flushCommands();
      }

      final Map<Long, Facility> facilityMap = CompletableFutures.successfulAsList(futures, error -> {
        LOGGER.error("Failed to get facility");
        return null;
      }).thenApply(results ->
          results.stream().filter(Objects::nonNull).collect(Collectors.toMap(Facility::getId, Function.identity())))
          .join();

      return ImmutableMap.copyOf(facilityMap);
    } catch (Exception e) {
      throw new IOException("Failed to get largest primary key from REDIS", e);
    }
  }

  @Override
  public long getLargestPrimaryKey() throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      final List<Long> pks = connection.sync().keys(KEY + ":*")
          .stream()
          .map(redisKey -> redisKey.split(":"))
          .map(redisKeySplit -> redisKeySplit[2])
          .map(pkStr -> Long.valueOf(pkStr, 10))
          .sorted((lhs, rhs) -> -1 * Long.compare(lhs, rhs))
          .collect(Collectors.toList());

      if (!pks.isEmpty()) {
        return pks.get(0);
      }
      return 0;
    } catch (Exception e) {
      throw new IOException("Failed to get largest primary key from REDIS", e);
    }
  }

  public void addFacility(final Facility facility) throws IOException {
    if (facility.getId() <= 0) {
      final long pk = generatePrimaryKey();
      facility.setId(pk);
    }

    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      final Map<String, String> stringStringMap = toStringMap(facility);
      connection.sync().hmset(KEY + ":" + facility.getId(), stringStringMap);
      indexFacility(connection.sync(), facility);
    } catch (Exception e) {
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }

  @Override
  public void updateFacility(Facility facility) throws IOException {
    if (facility.getId() <= 0) {
      final long pk = generatePrimaryKey();
      facility.setId(pk);
    }

    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      final Map<String, String> stringStringMap = toStringMap(facility);
      connection.sync().hmset(KEY + ":" + facility.getId(), stringStringMap);
      indexFacility(connection.sync(), facility);
    } catch (Exception e) {
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }

  public Facility getFacility(final String pk) throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      return getFacility(connection, pk);
    } catch (Exception e) {
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }

  public Facility getFacility(final StatefulRedisConnection<String, String> connection,
      final String pk) {
      return toFacility(connection.sync().hmget(KEY + ":" + pk, "_source"));
  }

  public CompletionStage<Facility> getFacilityAsync(final RedisAsyncCommands<String, String> asyncCommands,
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

  @Override
  public SearchResults<Facility> findByServiceCodes(List<String> serviceCodes, Page page)
      throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {

      final String searchKey = "s:" + connection.sync().incr(SEARCH_REQ);

      final String[] serviceCodeSets = serviceCodes
          .stream()
          .map(code -> INDEX_BY_SERVICES + ":" + code)
          .collect(Collectors.toSet()).toArray(new String[]{});

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

  private List<Facility> fetchBatch(final RedisAsyncCommands<String, String> asyncCommands, final List<String> ids) {
    if (asyncCommands == null || ids == null) {
      return new ArrayList<>(0);
    }

    // #5 Fetch each facility
    final List<CompletionStage<Facility>> facilityFutures =
        ids.stream().map(id -> getFacilityAsync(asyncCommands, id))
            .collect(Collectors.toList());

    final CompletableFuture<List<Facility>>
      batchFuture = CompletableFutures.successfulAsList(facilityFutures, t -> {
      LOGGER.error("Fetching one of the facilities failed", t);
      return null;
    }).thenApply(results -> results.stream().filter(Objects::nonNull).collect(Collectors.toList()));

    if (!LettuceFutures.awaitAll(Duration.ofSeconds(DEFAULT_TIMEOUT), batchFuture)) {
      return new ArrayList<>(0);
    }

    return batchFuture.getNow(new ArrayList<>(0));
  }

  @Override
  public SearchResults<Facility> findByServiceCodesWithin(List<String> serviceCodes,
      double longitude, double latitude, double distance, String geoUnit, Page page)
      throws IOException {

    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {

      final String searchId = connection.sync().incr(SEARCH_REQ) + "";
      final String searchKey = "s:" + searchId;
      final String radiusKey = "s:geo:" + searchId;

      final String[] serviceCodeSets = serviceCodes
          .stream()
          .map(code -> INDEX_BY_SERVICES + ":" + code)
          .collect(Collectors.toSet()).toArray(new String[]{});

      final RedisAsyncCommands<String, String> asyncCommands = connection.async();
      asyncCommands.multi();

      // TODO: I think we can do a lua script here to reduce round trips
      // #1 Find all of the centers within a certain radius

      RedisFuture<Long> geoRadiusFuture = asyncCommands
          .georadius(INDEX_BY_GEO, longitude, latitude, distance, Unit.valueOf(geoUnit),
              GeoRadiusStoreArgs.Builder
                  .store(radiusKey));

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
            .zrange(searchKey, page.offset(), page.offset() + page.size());

        final List<Facility> searchResults = fetchBatch(asyncCommands, ids);

        return SearchResults.searchResults(geoServicesIntersectionFuture.get(), searchResults);
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

  public void indexFacility(final Facility facility) throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      connection.setAutoFlushCommands(false);
      final RedisCommands<String, String> sync = connection.sync();
      try {
        indexFacility(sync, facility);
        connection.flushCommands();
      } finally {
        connection.setAutoFlushCommands(true);
      }
    } catch (Exception e) {
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }

  private void clearIndexForFacility(final RedisCommands<String, String> sync, final Facility original,
      final Facility newFacility) {
    sync.zrem(INDEX_BY_GEO, Long.toString(original.getId(), 10));

  }

  public void indexFacility(final RedisCommands<String, String> sync, final Facility facility)
      throws IOException {
    indexFacilityByGeo(sync, facility);
    sync.getStatefulConnection().flushCommands();

    indexFacilityByServiceCodes(sync, facility);
    sync.getStatefulConnection().flushCommands();

    indexFacilityByCategoryCodes(sync, facility);
    sync.getStatefulConnection().flushCommands();
  }

  private List<RedisFuture<Long>> indexFacilityByGeoAsync(
      final RedisAsyncCommands<String, String> async, final Facility facility) {
    if (facility == null) {
      return ImmutableList.of();
    }

    if (facility.getId() == 0) {
      throw new IllegalArgumentException("id must be non zero");
    }

    if (facility.getLocation() == null) {
      return ImmutableList.of();
    }

    return
        ImmutableList.of(async.geoadd(INDEX_BY_GEO, facility.getLocation().lon(),
            facility.getLocation().lat(),
            Long.toString(facility.getId(), 10)));
  }

  private void indexFacilityByGeo(final RedisCommands<String, String> sync, final Facility facility)
      throws IOException {
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
        .geoadd(INDEX_BY_GEO, facility.getLocation().lon(), facility.getLocation().lat(),
            Long.toString(facility.getId(), 10));

    LOGGER.debug("{} items added to set {}", geoAddCount, INDEX_BY_GEO);
  }

  public List<RedisFuture<Long>> indexFacilityByCategoryCodesAsync(
      final RedisAsyncCommands<String, String> async, final Facility facility) throws IOException {
    if (facility == null) {
      return ImmutableList.of();
    }

    if (facility.getId() == 0) {
      throw new IllegalArgumentException("id must be non zero");
    }

    return facility.getCategoryCodes()
        .stream()
        .filter(Objects::nonNull)
        .filter(it -> !it.isEmpty())
        .map(code -> async
            .sadd(INDEX_BY_CATEGORIES + ":" + code, Long.toString(facility.getId(), 10)))
        .collect(Collectors.toList());
  }

  public void indexFacilityByCategoryCodes(final RedisCommands<String, String> sync,
      final Facility facility) throws IOException {
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
          sync.sadd(INDEX_BY_CATEGORIES + ":" + code, Long.toString(facility.getId(), 10));
        });
  }

  public void indexFacilityByServiceCodes(final RedisCommands<String, String> sync,
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
          sync.sadd(INDEX_BY_SERVICES + ":" + code, Long.toString(facility.getId(), 10));
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
}
