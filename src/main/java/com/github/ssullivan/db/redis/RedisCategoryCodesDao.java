package com.github.ssullivan.db.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.model.Category;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.cache.LoadingCache;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RedisCategoryCodesDao implements ICategoryCodesDao {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisServiceCodeDao.class);

  private static final String KEY = "treatment:categories";

  private IRedisConnectionPool redis;
  private ObjectReader objectReader;
  private ObjectWriter objectWriter;

  private RedisCommands<String, String> sync;

  @Inject
  public RedisCategoryCodesDao(IRedisConnectionPool redisPool, ObjectMapper objectMapper) {
    this.redis = redisPool;
    this.objectReader = objectMapper.readerFor(Category.class);
    this.objectWriter = objectMapper.writerFor(Category.class);

    try {
      this.sync = redisPool.borrowConnection().sync();
    }
    catch (Exception e) {
      LOGGER.error("Failed to borrow a connection from the redis pool!", e);
      throw new RuntimeException("Failed to connect to Redis", e);
    }
  }


  @Override
  public Category get(String id) throws IOException {
    try {
      final String json = sync.hget(KEY, id);
      if (json == null || json.isEmpty()) {
        return null;
      }
      return deserialize(json);
    } catch (Exception e) {
      LOGGER.error("Failed to load category '{}'", id, e);
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new IOException("Failed to connect to REDIS", e);
    }
  }



  @Override
  public boolean delete(final String id) throws IOException {
    try {
      return sync.hdel(KEY, id) > 0;
    } catch (Exception e) {
      LOGGER.error("Failed to delete category '{}'", id, e);
      throw new IOException("Failed to connect to REDIS", e);
    }
  }

  @Override
  public Category getByCategoryCode(String categoryCode) throws IOException {
    return get(categoryCode);
  }

  @Override
  public List<String> listCategoryCodes() throws IOException {
    try {
      return sync.hkeys(KEY);
    } catch (Exception e) {
      throw new IOException("Failed to connect to REDIS", e);
    }
  }

  @Override
  public List<Category> listCategories() throws IOException {
    try {
      return sync.hvals(KEY)
          .stream()
          .map(it -> deserialize(it, null))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    } catch (Exception e) {
      LOGGER.error("Failed to list all categories", e);
      throw new IOException("Failed to connect to REDIS", e);
    }
  }

  @Override
  public boolean addCategory(String feed, Category category) throws IOException {
    try {
      return sync.hset(KEY, category.getCode(),
          serialize(category));
    } catch (Exception e) {
      LOGGER.error("Failed to store Category: {} in hset {}", category.getCode(), KEY);
      throw new IOException("Failed to connect to REDIS", e);
    }
  }


  private String serialize(@Nonnull final Category category) throws IOException {
    return objectWriter.writeValueAsString(category);
  }

  private Category deserialize(@Nonnull final String json) throws IOException {
    return objectReader.readValue(json);
  }

  private Category deserialize(@Nonnull final String json, final Category defaultValue) {
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
