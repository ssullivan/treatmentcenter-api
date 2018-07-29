package com.github.ssullivan.db.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.model.Category;
import io.lettuce.core.api.StatefulRedisConnection;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisCategoryCodesDao implements ICategoryCodesDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisServiceCodeDao.class);

  private static final String KEY = "treatment:categories";

  private IRedisConnectionPool redis;
  private ObjectReader objectReader;
  private ObjectWriter objectWriter;

  @Inject
  public RedisCategoryCodesDao(IRedisConnectionPool redisPool, ObjectMapper objectMapper) {
    this.redis = redisPool;
    this.objectReader = objectMapper.readerFor(Category.class);
    this.objectWriter = objectMapper.writerFor(Category.class);
  }

  @Override
  public Category get(String id) throws IOException {
    try (StatefulRedisConnection<String, String> connection = redis.borrowConnection()) {
      return deserialize(connection.sync().hget(KEY, id));
    } catch (Exception e) {
      throw new IOException("Failed to connect to REDIS", e);
    }
  }

  @Override
  public Category getByCategoryCode(String categoryCode) throws IOException {
    return get(categoryCode);
  }

  @Override
  public List<String> listCategoryCodes() throws IOException {
    try (StatefulRedisConnection<String, String> connection = redis.borrowConnection()) {
      return connection.sync().hkeys(KEY);
    } catch (Exception e) {
      throw new IOException("Failed to connect to REDIS", e);
    }
  }

  @Override
  public List<Category> listCategories() throws IOException {
    try (StatefulRedisConnection<String, String> connection = redis.borrowConnection()) {
      return connection.sync().hvals(KEY)
          .stream()
          .map(it -> deserialize(it, null))
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new IOException("Failed to connect to REDIS", e);
    }
  }

  @Override
  public boolean addCategory(Category category) throws IOException {
    try (StatefulRedisConnection<String, String> connection = redis.borrowConnection()) {
      return connection.sync().hset(KEY, category.getCode(),
          serialize(category));
    } catch (Exception e) {
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
    if (json.isEmpty()) return defaultValue;
    try {
      return deserialize(json);
    } catch (IOException e) {
      LOGGER.error("Failed to deserialize JSON for category", e);
      return defaultValue;
    }
  }

}
