package com.github.ssullivan.db.redis;

import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.utils.ShortUuid;
import com.google.inject.Singleton;
import io.lettuce.core.api.StatefulRedisConnection;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RedisFeedDao implements IFeedDao {

  public static final String CURRENT_FEED_KEY = "curr_feed";
  public static final String SEARCH_FEED_KEY = "search_feed";
  public static final String FEED_IDS_KEY = "feed_ids";
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisFeedDao.class);
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
    LOGGER.info("SET {} to {}", CURRENT_FEED_KEY, id);
    try (StatefulRedisConnection<String, String> redis = pool.borrowConnection()) {
      Optional<String> result = Optional.ofNullable(redis.sync().set(CURRENT_FEED_KEY, id));
      redis.sync().sadd(FEED_IDS_KEY, id);
      return result;
    } catch (Exception e) {
      handleException(e);
    }
    return Optional.empty();
  }

  @Override
  public Optional<String> setSearchFeedId(String id) throws IOException {
    LOGGER.info("SET {} to {}", SEARCH_FEED_KEY, id);
    try (StatefulRedisConnection<String, String> redis = pool.borrowConnection()) {
      int retries = 3;
      do {
        try {
          return Optional.ofNullable(redis.sync().set(SEARCH_FEED_KEY, id));
        } catch (Exception e) {
          LOGGER.error("Failed to set search feed id {}", id, e);
        }
      } while (retries-- > 0);
    } catch (Exception e) {
      handleException(e);
    }
    return Optional.empty();
  }

  @Override
  public Collection<String> getFeedIds() throws IOException {
    try (StatefulRedisConnection<String, String> redis = pool.borrowConnection()) {
      return redis.sync().smembers(FEED_IDS_KEY);
    } catch (Exception e) {
      handleException(e);
    }
    return new HashSet<>();
  }

  @Override
  public void removeFeedId(String feedId) throws IOException {
    try (StatefulRedisConnection<String, String> redis = pool.borrowConnection()) {
      redis.sync().srem(FEED_IDS_KEY, feedId);
    } catch (Exception e) {
      handleException(e);
    }
  }

  @Override
  public Optional<String> currentFeedId() throws IOException {
    return fetchkey(CURRENT_FEED_KEY);
  }

  @Override
  public Optional<String> searchFeedId() throws IOException {
    return fetchkey(SEARCH_FEED_KEY);
  }

  private Optional<String> fetchkey(final String key) throws IOException {
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
    } else {
      throw new IOException("Failed to get next feed id", e);
    }
  }
}
