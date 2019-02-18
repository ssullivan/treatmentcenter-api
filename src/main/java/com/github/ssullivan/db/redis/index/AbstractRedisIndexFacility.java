package com.github.ssullivan.db.redis.index;

import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IndexFacility;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.model.Facility;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRedisIndexFacility implements IndexFacility {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRedisIndexFacility.class);

  protected IRedisConnectionPool pool;
  protected IFeedDao feedDao;

  AbstractRedisIndexFacility(IRedisConnectionPool pool, IFeedDao feedDao) {
    this.pool = pool;
    this.feedDao = feedDao;
  }


  @Override
  public void index(String feed, Facility facility) throws IOException {
    try (StatefulRedisConnection<String, String> connection = this.pool.borrowConnection()) {
      index(connection.sync(), feed, facility);
    } catch (Exception e) {
      handleException(e);
    }
  }

  @Override
  public void index(String feed, List<Facility> batch) throws IOException {
    try (StatefulRedisConnection<String, String> connection = this.pool.borrowConnection()) {
      batch.forEach(item -> index(connection.sync(), feed, item));
    } catch (Exception e) {
      handleException(e);
    }
  }

  private void handleException(final Exception e) throws IOException {
    if (e instanceof InterruptedException) {
      LOGGER.error("Interrupted while indexing", e);
      Thread.currentThread().interrupt();
    } else {
      LOGGER.error("Failed to index facility", e);
    }
    throw new IOException("Failed to index facility", e);
  }

  abstract protected void index(final RedisCommands<String, String> sync, final String feed,
      final Facility facility);

  void expireMatching(final String pattern, final long seconds) throws IOException {
    Objects.requireNonNull(pattern, "Key pattern must not be null");
    if (pattern.isEmpty()) {
      throw new IllegalArgumentException("Key pattern must not be empty");
    }

    try (final StatefulRedisConnection<String, String> connection = this.pool.borrowConnection()) {
      KeyScanCursor<String> cursor = connection.sync().scan(
          ScanArgs.Builder.matches(pattern));
      while (!cursor.isFinished()) {
        for (final String key : cursor.getKeys()) {
          connection.sync().expire(key, seconds);
        }
        cursor = connection.sync().scan(cursor);
      }

    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        LOGGER.error("Interrupted while expiring keys for {}", pattern, e);
        Thread.currentThread().interrupt();
      }
      throw new IOException("Failed to get connection to REDIS", e);
    }
  }
}
