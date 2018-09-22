package com.github.ssullivan.db.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.Pair;
import com.github.ssullivan.model.SearchResults;
import com.google.common.collect.ImmutableSet;
import com.spotify.futures.CompletableFutures;
import io.lettuce.core.GeoArgs;
import io.lettuce.core.GeoArgs.Unit;
import io.lettuce.core.GeoRadiusStoreArgs;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.ScoredValueScanCursor;
import io.lettuce.core.ZStoreArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisFacilityDao implements IFacilityDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisFacilityDao.class);

  private static final String SEARCH_REQ = "search:counter";

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

  public void addFacility(final Facility facility) throws IOException {
    final long pk = generatePrimaryKey();
    facility.setId(pk);

    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      final Map<String, String> stringStringMap = toStringMap(facility);
      connection.sync().hmset(KEY + ":" + pk, stringStringMap);
      indexFacility(connection.sync(), facility);
    } catch (Exception e) {
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }

  public Facility getFacility(final StatefulRedisConnection<String, String> connection, final String pk) {
    List<KeyValue<String, String>> fields = connection.sync().hmget(KEY + ":" + pk, "_source");
    if (fields != null && !fields.isEmpty()) {
      return deserialize(fields.get(0).getValue(), null);
    }
    return null;
  }

  @Override
  public SearchResults<Facility> findByServiceCodes(List<String> serviceCodes, Page page)
      throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {

      final String searchKey = "search:" + connection.sync().incr(SEARCH_REQ);


      final String[] serviceCodeSets = serviceCodes
          .stream()
          .map(code -> INDEX_BY_SERVICES + ":" + code)
          .collect(Collectors.toSet()).toArray(new String[]{});

      final Long numResults = connection.sync().zunionstore(searchKey, serviceCodeSets);

      final List<String> ids = connection.sync().zrange(searchKey, page.offset(), page.offset() + page.size());
      connection.sync().expire(searchKey, 5);

      final List<Facility> searchResults = ids.stream().map(id -> getFacility(connection, id))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

      connection.sync().del(searchKey);


      return SearchResults.searchResults(numResults, searchResults);
    } catch (Exception e) {
      LOGGER.error("Failed to find any facilities with serviceCodes: {}, page: {}", serviceCodes, page);
      throw new IOException("Failed to find any matching results", e);
    }
  }

  @Override
  public SearchResults<Facility> findByServiceCodesWithin(List<String> serviceCodes,
      double longitude, double latitude, double distance, String geoUnit, Page page)
      throws IOException {

    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {

      final String searchId = connection.sync().incr(SEARCH_REQ) + "";
      final String searchKey = "search:" + searchId;
      final String radiusKey = "search:geo:" + searchId;


      final String[] serviceCodeSets = serviceCodes
          .stream()
          .map(code -> INDEX_BY_SERVICES + ":" + code)
          .collect(Collectors.toSet()).toArray(new String[]{});

      connection.sync().multi();

      // TODO: I think we can do a lua script here to reduce round trips
      // #1 Find all of the centers within a certain radius

      connection.sync().georadius(INDEX_BY_GEO, longitude, latitude, distance, Unit.valueOf(geoUnit),
          GeoRadiusStoreArgs.Builder.store(INDEX_BY_GEO));

      // #2 Find all of the centers with the specified services
      connection.sync().zunionstore(searchKey, serviceCodeSets);

      // #3 Find the intersection of the places that have our services we want and
      //    are within a specific radius
      final Long numResults = connection.sync()
          .zinterstore(searchKey + ":" + 1, searchKey, radiusKey);

      // #4 Fetch the results
      final List<String> ids = connection.sync()
          .zrange(searchKey, page.offset(), page.offset() + page.size());

      // #5 Fetch each facility
      final List<Facility> searchResults = ids.stream().map(id -> getFacility(connection, id))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

      // #6 Ensure the keys get deleted
      connection.sync().expire(searchKey, 5);
      connection.sync().expire(searchKey + ":1", 5);
      connection.sync().expire(radiusKey, 5);
      connection.sync().exec();

      // #7 Explicitly delete the keys
      connection.sync().del(searchKey, searchKey + ":1", radiusKey);

      return SearchResults.searchResults(numResults, searchResults);

    } catch (Exception e) {
      LOGGER.error("Failed to find any facilities with serviceCodes: {}, page: {}", serviceCodes, page);
      throw new IOException("Failed to find any matching results", e);
    }
  }

  public void indexFacility(final Facility facility) throws IOException{
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
        connection.setAutoFlushCommands(false);
        final RedisCommands<String, String> sync = connection.sync();
        try {
          indexFacility(sync, facility);
          connection.flushCommands();
        }
        finally {
          connection.setAutoFlushCommands(true);
        }
    } catch (Exception e) {
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }

  public void indexFacility(final RedisCommands<String, String> sync, final Facility facility) throws IOException{
      indexFacilityByGeo(sync, facility);
      sync.getStatefulConnection().flushCommands();

      indexFacilityByServiceCodes(sync, facility);
      sync.getStatefulConnection().flushCommands();

      indexFacilityByCategoryCodes(sync, facility);
      sync.getStatefulConnection().flushCommands();
  }

  private void indexFacilityByGeo(final RedisCommands<String, String> sync, final Facility facility) throws IOException {
    if (facility == null) {
      return;
    }

    if (facility.getId() == 0) {
      throw new IllegalArgumentException("id must be non zero");
    }

    if (facility.getLocation() == null) {
      return;
    }


    sync.geoadd(INDEX_BY_GEO, facility.getLocation().lon(), facility.getLocation().lat(),  Long.toString(facility.getId(), 10));
  }

  public void indexFacilityByCategoryCodes(final RedisCommands<String, String> sync, final Facility facility) throws IOException {
    if (facility == null) return;

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

  public void indexFacilityByServiceCodes(final RedisCommands<String, String> sync, final Facility facility) throws IOException {
    if (facility == null) return;

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

    return null;
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

    toReturn.put("location.lat", "" + facility.getLocation().lat());
    toReturn.put("location.lon", "" + facility.getLocation().lon());

    return toReturn;
  }

  private String serialize(@Nonnull final Facility category) throws IOException {
    return objectWriter.writeValueAsString(category);
  }

  private Facility deserialize(@Nonnull final String json) throws IOException {
    return objectReader.readValue(json);
  }

  private Facility deserialize(@Nonnull final String json, final Facility defaultValue) {
    if (json.isEmpty()) return defaultValue;
    try {
      return deserialize(json);
    } catch (IOException e) {
      LOGGER.error("Failed to deserialize JSON for category", e);
      return defaultValue;
    }
  }
}
