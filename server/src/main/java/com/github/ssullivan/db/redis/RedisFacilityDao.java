package com.github.ssullivan.db.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.model.Category;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.google.common.collect.ImmutableSet;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.RedisCommand;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisFacilityDao implements IFacilityDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisFacilityDao.class);


  private static final String KEY = "treatment:facilities";
  private static final String PK_KEY = "treatment:facilities:counter";
  private static final String INDEX_BY_SERVICES = "index:facility_by_service";
  private static final String INDEX_BY_CATEGORIES = "index:facility_by_category";
  private static final String INDEX_BY_GEO = "index:facility_by_geo";

  private IRedisConnectionPool redis;
  private ObjectReader objectReader;
  private ObjectWriter objectWriter;

  @Inject
  public RedisFacilityDao(IRedisConnectionPool connectionPool, ObjectMapper objectMapper) {
    this.redis = connectionPool;
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
      connection.sync().set(KEY + ":" + pk, serialize(facility));
      indexFacility(connection.sync(), facility);
    } catch (Exception e) {
      throw new IOException("Failed to get connection to REDIS", e);
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
      indexFacilityByServiceCodes(sync, facility);
      indexFacilityByCategoryCodes(sync, facility);
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


    sync.geoadd(INDEX_BY_GEO, facility.getLocation().lat(), facility.getLocation().lon(), facility.getId());
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
        .forEach(serviceCode -> {
          sync.set(INDEX_BY_CATEGORIES + ":" + serviceCode, Long.toString(facility.getId(), 10));
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
            .forEach(serviceCode -> {
              sync.set(INDEX_BY_SERVICES + ":" + serviceCode, Long.toString(facility.getId(), 10));
            });
  }

  @Override
  public SearchResults findByServiceCodes(ImmutableSet<String> mustServiceCodes,
      Page page) throws IOException {
    return null;
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
