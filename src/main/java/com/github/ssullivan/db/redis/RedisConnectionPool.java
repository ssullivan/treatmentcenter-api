package com.github.ssullivan.db.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RedisConnectionPool implements IRedisConnectionPool {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisConnectionPool.class);
  private static final long MAX_WAIT_MILLIS = 100;

  private RedisClient redisClient;
  private GenericObjectPool<StatefulRedisConnection<String, String>> pool;
  private AtomicBoolean isClosed = new AtomicBoolean(false);

  @Inject
  public RedisConnectionPool(RedisClient redisClient) {
    this.redisClient = redisClient;

    GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
    genericObjectPoolConfig.setMaxTotal(20);
    genericObjectPoolConfig.setMaxWaitMillis(MAX_WAIT_MILLIS);

    this.pool = ConnectionPoolSupport
        .createGenericObjectPool(redisClient::connect, genericObjectPoolConfig);
  }

  @Override
  public StatefulRedisConnection<String, String> borrowConnection() throws Exception {

    return pool.borrowObject();
  }

  @Override
  public StatefulRedisConnection<String, String> borrowConnection(long maxWaitMillis)
      throws Exception {
    return pool.borrowObject(maxWaitMillis);
  }

  @Override
  public boolean isClosed() {
    return this.isClosed.get();
  }

  @Override
  public void close() {
    if (isClosed.compareAndSet(false, true)) {
      try {
        this.pool.close();
      } finally {
        this.redisClient.shutdown();
      }
    } else {
      LOGGER.warn("Pool has already been closed");
    }
  }
}
