package com.github.ssullivan.db.redis.index;

import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IndexFacility;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.model.Facility;
import com.sun.org.apache.bcel.internal.generic.IFEQ;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractRedisIndexFacility implements IndexFacility {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRedisIndexFacility.class);

  protected IRedisConnectionPool pool;
  protected IFeedDao feedDao;

  public AbstractRedisIndexFacility(IRedisConnectionPool pool, IFeedDao feedDao) {
    this.pool = pool;
    this.feedDao = feedDao;
  }


  @Override
  public void index(String feed, Facility facility) throws IOException {
    try (StatefulConnection<String, String> connection = this.pool.borrowConnection()) {
      index(((StatefulRedisConnection<String, String>) connection).sync(), feed, facility);
    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        LOGGER.error("Interrupted while indexing", e);
        Thread.currentThread().interrupt();
      }
      throw new IOException("Failed to index facility", e);
    }
  }

  abstract protected  void index(final RedisCommands<String, String> sync, final String feed, final Facility facility);
}
