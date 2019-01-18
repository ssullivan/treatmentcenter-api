package com.github.ssullivan.db.redis;

import com.google.inject.ImplementedBy;
import io.lettuce.core.api.StatefulRedisConnection;
import java.io.Closeable;

@ImplementedBy(RedisConnectionPool.class)
public interface IRedisConnectionPool  {
  StatefulRedisConnection<String, String> borrowConnection() throws Exception;

  boolean isClosed();

  void close();
}
