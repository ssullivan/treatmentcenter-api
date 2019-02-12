package com.github.ssullivan.db.redis;

import com.google.inject.ImplementedBy;
import io.lettuce.core.api.StatefulRedisConnection;
import java.util.concurrent.TimeUnit;

@ImplementedBy(RedisConnectionPool.class)
public interface IRedisConnectionPool {

  StatefulRedisConnection<String, String> borrowConnection() throws Exception;

  StatefulRedisConnection<String, String> borrowConnection(long duration, TimeUnit unit)
      throws Exception;

  boolean isClosed();

  void close();
}
