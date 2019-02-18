package com.github.ssullivan.db.redis;

import com.google.inject.ImplementedBy;
import io.lettuce.core.api.StatefulRedisConnection;

@ImplementedBy(RedisConnectionPool.class)
public interface IRedisConnectionPool {

  StatefulRedisConnection<String, String> borrowConnection() throws Exception;


  StatefulRedisConnection<String, String> borrowConnection(final long maxWaitMillis)
      throws Exception;

  boolean isClosed();

  void close();
}
