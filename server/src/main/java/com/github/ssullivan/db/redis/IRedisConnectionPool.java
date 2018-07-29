package com.github.ssullivan.db.redis;

import io.lettuce.core.api.StatefulRedisConnection;

public interface IRedisConnectionPool {
  StatefulRedisConnection<String, String> borrowConnection() throws Exception;

  void close();
}
