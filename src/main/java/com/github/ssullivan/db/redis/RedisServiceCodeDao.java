package com.github.ssullivan.db.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.ssullivan.db.IServiceCodesDao;
import com.github.ssullivan.model.Category;
import com.github.ssullivan.model.Service;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.cache.LoadingCache;
import io.lettuce.core.api.StatefulRedisConnection;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisServiceCodeDao implements IServiceCodesDao {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisServiceCodeDao.class);

  private static final String KEY = "treatment:services";

  private IRedisConnectionPool redis;
  private ObjectReader serviceReader;
  private ObjectWriter serviceWriter;
  private LoadingCache<String, Service> cache =
      CacheBuilder.newBuilder()
          .maximumSize(1024)
          .concurrencyLevel(8)
          .expireAfterAccess(60, TimeUnit.MINUTES)
          .build(new CacheLoader<String, Service>() {
            @Override
            public Service load(final String key) throws Exception {
              return get(key);
            }
          });

  @Inject
  public RedisServiceCodeDao(IRedisConnectionPool redisPool, ObjectMapper objectMapper) {
    this.redis = redisPool;
    this.serviceReader = objectMapper.readerFor(Service.class);
    this.serviceWriter = objectMapper.writerFor(Service.class);
  }

  @Override
  public Service get(String id) throws IOException {
    try (StatefulRedisConnection<String, String> connection = redis.borrowConnection()) {
      return deserialize(connection.sync().hget(KEY, id));
    } catch (Exception e) {
      throw new IOException("Failed to connect to REDIS", e);
    }
  }

  @Override
  public Service get(String id, boolean fromCache) throws IOException {
    try {
      if (fromCache)
        return this.cache.get(id);
      else
        return this.get(id);
    } catch (InvalidCacheLoadException e) {
      LOGGER.error("Failed to get service code '{}' from db", id, e);
      throw new IOException("Failed to load service code", e);
    } catch (ExecutionException e) {
      LOGGER.error("Failed to get Category '{}' from in-memory cache", id, e);
      throw new IOException(e);
    }
  }

  @Override
  public boolean delete(String id) throws IOException {
    try (StatefulRedisConnection<String, String> connection = redis.borrowConnection()) {
      return connection.sync().hdel(KEY, id) > 0;
    } catch (Exception e) {
      throw new IOException("Failed to connect to REDIS", e);
    }
  }

  @Override
  public Service getByServiceCode(String serviceCode) throws IOException {
    return get(serviceCode);
  }

  @Override
  public List<Service> listServices() throws IOException {
    try (StatefulRedisConnection<String, String> connection = redis.borrowConnection()) {
      return deserializeSuccessfulAsList(connection.sync().hvals(KEY));
    } catch (Exception e) {
      throw new IOException("Failed to connect to REDIS", e);
    }
  }

  @Override
  public List<String> listServiceCodes() throws IOException {
    try (StatefulRedisConnection<String, String> connection = redis.borrowConnection()) {
      return connection.sync().hkeys(KEY);
    } catch (Exception e) {
      throw new IOException("Failed to connect to REDIS", e);
    }
  }

  @Override
  public List<String> listServiceCodesInCategory(String category) throws IOException {
    try (StatefulRedisConnection<String, String> connection = redis.borrowConnection()) {
      return connection.sync().hvals(KEY)
          .stream()
          .map(json -> {
            try {
              return deserialize(json);
            } catch (IOException e) {
              LOGGER.error("Failed to deserialize service", e);
            }
            return null;
          })
          .filter(Objects::nonNull)
          .filter(service -> service.getCategoryCode().equalsIgnoreCase(category))
          .map(Service::getCode)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new IOException("Failed to connect to REDIS", e);
    }
  }

  @Override
  public boolean addService(final Service service) throws IOException {
    try (StatefulRedisConnection<String, String> connection = redis.borrowConnection()) {
      return connection.sync().hset(KEY, service.getCode(),
          serialize(service));
    } catch (Exception e) {
      throw new IOException("Failed to connect to REDIS", e);
    }
  }

  private String serialize(@Nonnull final Service service) throws IOException {
    return serviceWriter.writeValueAsString(service);
  }

  private Service deserialize(@Nonnull final String json) throws IOException {
    return serviceReader.readValue(json);
  }

  private List<Service> deserializeSuccessfulAsList(@Nonnull final List<String> jsons) {
    return jsons.stream().map(it -> {
      try {
        return deserialize(it);
      } catch (IOException e) {
        LOGGER.error("Failed to deserialize service json", e);
        return null;
      }
    })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }
}
