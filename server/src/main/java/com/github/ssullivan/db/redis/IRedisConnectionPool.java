package com.github.ssullivan.db.redis;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

public interface IRedisConnectionPool {
  StatefulRedisConnection<String, String> borrowConnection() throws Exception;

  void close();
}
