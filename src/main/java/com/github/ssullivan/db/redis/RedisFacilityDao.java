package com.github.ssullivan.db.redis;

import static com.github.ssullivan.db.redis.RedisConstants.DEFAULT_TIMEOUT;
import static com.github.ssullivan.db.redis.RedisConstants.DEFAULT_TIMEOUT_UNIT;
import static com.github.ssullivan.db.redis.RedisConstants.TREATMENT_FACILITIES;
import static com.github.ssullivan.db.redis.RedisConstants.TREATMENT_FACILITIES_IDS;
import static com.github.ssullivan.db.redis.RedisConstants.isValidIdentifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IServiceCodesDao;
import com.github.ssullivan.db.IndexFacility;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.utils.ShortUuid;
import com.google.inject.Singleton;
import com.spotify.futures.CompletableFutures;
import io.lettuce.core.KeyValue;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RedisFacilityDao implements IFacilityDao {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisFacilityDao.class);


  private IRedisConnectionPool redis;
  private IAsyncRedisConnectionPool asyncPool;
  private ICategoryCodesDao categoryCodesDao;
  private IServiceCodesDao serviceCodesDao;
  private IFeedDao feedDao;
  private FacilityToMapConverter facilityToMapConverter;
  private ObjectReader reader;
  private ObjectWriter writer;
  private IndexFacility indexFacility;

  @Inject
  public RedisFacilityDao(IRedisConnectionPool connectionPool,
      IAsyncRedisConnectionPool asyncConnectionPool,
      ICategoryCodesDao categoryCodesDao,
      IServiceCodesDao serviceCodesDao,
      IFeedDao feedDao,
      FacilityToMapConverter facilityToMapConverter,
      IndexFacility indexFacility,
      ObjectMapper objectMapper) {
    this.asyncPool = asyncConnectionPool;
    this.categoryCodesDao = categoryCodesDao;
    this.serviceCodesDao = serviceCodesDao;
    this.redis = connectionPool;
    this.feedDao = feedDao;
    this.facilityToMapConverter = facilityToMapConverter;
    this.reader = objectMapper.readerFor(Facility.class);
    this.writer = objectMapper.writerFor(Facility.class);
    this.indexFacility = indexFacility;

  }

  private static String facilityKey(final String id) {
    if (id == null) {
      throw new NullPointerException("Id must not be null");
    }
    return TREATMENT_FACILITIES + ":" + id;
  }

  private String generatePrimaryKey() throws IOException {
    return ShortUuid.randomShortUuid();
  }

  public List<Facility> list(final Page page) {
    return new ArrayList<>();
  }

  private void addFacility(final RedisCommands<String, String> redis, Facility facility)
      throws IOException {
    if (!isValidIdentifier(facility.getId())) {
      facility.setId(generatePrimaryKey());
    }

    final Map<String, String> stringStringMap = toStringMap(facility);
    redis.hmset(facilityKey(facility.getId()), stringStringMap);

    // this is so we can quickly delete stuff in the future
    redis.sadd(TREATMENT_FACILITIES_IDS + facility.getFeedId(), facility.getId());
    LOGGER.debug("Loaded Facility {} for feed {}", facility.getId(), facility.getFeedId());
  }


  @Override
  public void addFacility(String feedId, Facility facility) throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      facility.setFeedId(feedId);
      addFacility(connection.sync(), facility);
    } catch (Exception e) {
      LOGGER.error("Failed to add facility: {} for feed {}", facility, feedId);
      throw new IOException("Failed to get connection to REDIS", e);
    }

    indexFacility.index(feedId, facility);
  }

  @Override
  public void addFacility(String feedId, List<Facility> batch) throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      final RedisCommands<String, String> sync = connection.sync();
      for (Facility item : batch) {

        item.setFeedId(feedId);
        addFacility(sync, item);
      }
      indexFacility.index(feedId, batch);
    } catch (Exception e) {
      LOGGER.error("Failed to add facility batch: {} for feed {}", batch, feedId);
      throw new IOException("Failed to get connection to REDIS", e);
    }


  }

  public Facility getFacility(final String pk) throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      return getFacility(connection, pk);
    } catch (Exception e) {
      LOGGER.error("Failed to fetch facility", e);
      if (e instanceof InterruptedException) {
        LOGGER.error("Interrupted while fetching facility {}", pk);
        Thread.currentThread().interrupt();
      }
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }

  @Override
  public List<Facility> fetchBatch(final Collection<String> ids) {
    try {
      return this.fetchBatchAsync(ids)
          .toCompletableFuture()
          .get(250, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      LOGGER.error("Timeout waiting for locations", e);
    } catch (InterruptedException e) {
      LOGGER.error("Interrupted waiting for locations", e);
    } catch (ExecutionException e) {
      LOGGER.error("An exception occurred while fetching locations", e);
    }
    return new ArrayList<>();
  }

  @Override
  public CompletionStage<List<Facility>> fetchBatchAsync(final Collection<String> ids) {
    return this.asyncPool
        .borrowConnection()
        .thenCompose(it -> fetchBatchAsync(it.async(), ids)
            .whenComplete((s, error) -> {
              if (error != null) {
                LOGGER.error("fetchBatchAsync had error while fetch batch", error);
              }
              asyncPool.relase(it);
            }));
  }

  private Facility getFacility(final StatefulRedisConnection<String, String> connection,
      final String pk) {
    return toFacility(connection.sync().hmget(TREATMENT_FACILITIES + ":" + pk, "_source"));
  }

  private CompletionStage<Facility> getFacilityAsync(
      final RedisAsyncCommands<String, String> asyncCommands,
      final String pk) {

    return asyncCommands.hmget(TREATMENT_FACILITIES + ":" + pk, "_source")
        .thenApply(this::toFacility);
  }

  private Facility toFacility(final List<KeyValue<String, String>> fields) {
    if (fields != null && !fields.isEmpty()) {
      return fields.get(0).map(it -> {
        try {
          return deserialize(it);
        } catch (IOException e) {
          LOGGER.error("Failed to deserialize JSON", e);
        }
        return null;
      }).getValueOrElse(null);
    }
    return null;
  }


  private CompletionStage<List<Facility>> fetchBatchAsync(
      final RedisAsyncCommands<String, String> asyncCommands, final Collection<String> ids) {
    if (asyncCommands == null || ids == null) {
      return CompletableFuture.completedFuture(new ArrayList<>(0));
    }

    final Set<String> distinctIdentifiers = new TreeSet<>(new HashSet<>(ids));
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

  private List<Facility> fetchBatch(final RedisAsyncCommands<String, String> asyncCommands,
      final Collection<String> ids) {
    if (asyncCommands == null || ids == null) {
      return new ArrayList<>(0);
    }

    final CompletableFuture<List<Facility>> batchFuture =
        fetchBatchAsync(asyncCommands, ids)
            .toCompletableFuture();

    try {
      return batchFuture.get(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT);
    } catch (InterruptedException e) {
      LOGGER.error("Interrupted while fetching facilities", e);
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      LOGGER.error("Exception occurred while fetching facilities", e);
    } catch (TimeoutException e) {
      LOGGER.error("Timed out while while fetching facilities", e);
    }
    return batchFuture.getNow(new ArrayList<>(0));
  }

  private Map<String, String> toStringMap(final Facility facility) {
    return this.facilityToMapConverter.apply(facility);
  }

  private String serialize(@Nonnull final Facility facility) throws IOException {
    return writer.writeValueAsString(facility);
  }

  private Facility deserialize(@Nonnull final String json) throws IOException {
    return reader.readValue(json);
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


  @Override
  public Set<String> getKeysForFeed(String feedId) throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      return connection.sync().smembers(TREATMENT_FACILITIES_IDS + feedId);
    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        LOGGER.error("Interrupted while fetching facility {}", feedId);
        Thread.currentThread().interrupt();
      }
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }

  @Override
  public Boolean expire(String id, long seconds) throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      return connection.sync().expire(facilityKey(id), seconds);

    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        LOGGER.error("Interrupted while fetching facility {}", id);
        Thread.currentThread().interrupt();
      }
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }

  @Override
  public Boolean expire(String feed, long seconds, boolean overwrite) throws IOException {
    try (final StatefulRedisConnection<String, String> connection = this.redis.borrowConnection()) {
      final RedisCommands<String, String> sync = connection.sync();

      final Set<String> facilityIds = getKeysForFeed(feed);
      for (final String id : facilityIds) {
        final String keyToExpire = facilityKey(id);

        if (overwrite) {
          sync.expire(keyToExpire, seconds);
        } else {
          final Long ttl = sync.ttl(keyToExpire);
          if (ttl == null || ttl < 0) {
            sync.expire(keyToExpire, seconds);
          }
        }
      }

      if (overwrite) {
        sync.expire(TREATMENT_FACILITIES_IDS + feed, seconds);
      } else {
        final Long ttl = sync.ttl(TREATMENT_FACILITIES_IDS + feed);
        if (ttl == null || ttl < 0) {
          sync.expire(TREATMENT_FACILITIES_IDS + feed, seconds);
        }
      }

      return true;
    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        LOGGER.error("Interrupted while fetching facility", e);
        Thread.currentThread().interrupt();
      }
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }
}
