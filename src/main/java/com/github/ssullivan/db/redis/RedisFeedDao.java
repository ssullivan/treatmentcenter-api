package com.github.ssullivan.db.redis;

import com.github.ssullivan.db.IFeedDao;
import io.lettuce.core.api.StatefulRedisConnection;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisFeedDao implements IFeedDao {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisFeedDao.class);

  private static final String FEED_KEY = "FEED_COUNTER";
  private IRedisConnectionPool pool;

  @Inject
  public RedisFeedDao(IRedisConnectionPool redisConnectionPool) {
    this.pool = redisConnectionPool;
  }

  @Override
  public Optional<String> nextFeedId() throws IOException {
    try (StatefulRedisConnection<String, String> redis = pool.borrowConnection()) {
      return Optional.of("" + redis.sync().incr(FEED_KEY));
    } catch (Exception e) {
      handleException(e);
    }
    return Optional.empty();
  }

  @Override
  public Optional<String> currentFeedId() throws IOException {
    try (StatefulRedisConnection<String, String> redis = pool.borrowConnection()) {
      return Optional.of(redis.sync().get(FEED_KEY));
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
