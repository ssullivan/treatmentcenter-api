package com.github.ssullivan.db.redis;

import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.utils.ShortUuid;
import io.lettuce.core.api.StatefulRedisConnection;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisFeedDao implements IFeedDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisFeedDao.class);

  private static final String CURRENT_FEED_KEY = "CURR_FEED";
  private static final String SEARCH_FEED_KEY = "SEARCH_FEED";

  private IRedisConnectionPool pool;

  @Inject
  public RedisFeedDao(IRedisConnectionPool redisConnectionPool) {
    this.pool = redisConnectionPool;
  }

  @Override
  public Optional<String> nextFeedId() {
    return Optional.of(ShortUuid.randomShortUuid());
  }

  @Override
  public Optional<String> setCurrentFeedId(String id) throws IOException {
    try (StatefulRedisConnection<String, String> redis = pool.borrowConnection()) {
      return Optional.ofNullable(redis.sync().set(CURRENT_FEED_KEY, id));
    } catch (Exception e) {
      handleException(e);
    }
    return Optional.empty();
  }

  @Override
  public Optional<String> currentFeedId() throws IOException {
    return fetchUuid(CURRENT_FEED_KEY);
  }

  @Override
  public Optional<String> searchFeedId() throws IOException {
    return fetchUuid(SEARCH_FEED_KEY);
  }

  private Optional<String> fetchUuid(final String key) throws IOException {
    try (StatefulRedisConnection<String, String> redis = pool.borrowConnection()) {
      return Optional.ofNullable(redis.sync().get(key));
    } catch (Exception e) {
      handleException(e);
    }
    return Optional.empty();
  }

  private void handleException(final Exception e) throws IOException {
    if (e instanceof InterruptedException) {
      LOGGER.error("Interrupted while generating feed id", e);
      Thread.currentThread().interrupt();
    }
    else {
      throw new IOException("Failed to get next feed id", e);
    }
  }
}
