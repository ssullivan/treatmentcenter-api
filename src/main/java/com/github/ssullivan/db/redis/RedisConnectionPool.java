package com.github.ssullivan.db.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import javax.inject.Inject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class RedisConnectionPool implements IRedisConnectionPool {
  private RedisClient redisClient;
  private GenericObjectPool<StatefulRedisConnection<String, String>> pool;

  @Inject
  public RedisConnectionPool(RedisClient redisClient) {
    this.redisClient = redisClient;
    this.pool = ConnectionPoolSupport
        .createGenericObjectPool(redisClient::connect, new GenericObjectPoolConfig());
  }

  @Override
  public StatefulRedisConnection<String, String> borrowConnection() throws Exception {
    return pool.borrowObject();
  }

  @Override
  public void close() {
    try {
      this.pool.close();
    }
    finally {
      this.redisClient.shutdown();
    }
  }
}
