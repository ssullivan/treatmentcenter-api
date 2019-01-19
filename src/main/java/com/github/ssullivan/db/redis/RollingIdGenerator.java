package com.github.ssullivan.db.redis;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.api.StatefulRedisConnection;
import javax.inject.Inject;

public class RollingIdGenerator {

  private IRedisConnectionPool redisConnectionPool;

  @Inject
  RollingIdGenerator(IRedisConnectionPool redisConnectionPool) {

    this.redisConnectionPool = redisConnectionPool;
  }

  public Long generateId(final String key) throws Exception {
    try (StatefulRedisConnection<String, String> conn = this.redisConnectionPool.borrowConnection()) {
      try {
        return conn.sync().incr(key);
      } catch (RedisCommandExecutionException e) {
        return handleOverflow(conn, key, e);
      }
    }
  }

  public Long generateId(final String key, long ammount) throws Exception {
    try (StatefulRedisConnection<String, String> conn = this.redisConnectionPool.borrowConnection()) {
      try {
        return conn.sync().incrby(key, ammount);
      } catch (RedisCommandExecutionException e) {
        return handleOverflow(conn, key, e);
      }
    }
  }

  private Long handleOverflow(final StatefulRedisConnection<String, String> conn, final String key, RedisCommandExecutionException e) throws Exception {
    final String msg = e.getMessage();
    if ("ERR increment or decrement would overflow".equalsIgnoreCase(msg)) {
      conn.sync().del(key);
      return conn.sync().incr(key);
    }
    throw e;
  }
}
