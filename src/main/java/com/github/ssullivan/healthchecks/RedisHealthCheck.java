package com.github.ssullivan.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import io.lettuce.core.api.StatefulRedisConnection;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisHealthCheck extends HealthCheck {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisHealthCheck.class);

  private IRedisConnectionPool redisConnectionPool;

  @Inject
  public RedisHealthCheck(IRedisConnectionPool redisConnectionPool) {
    this.redisConnectionPool = redisConnectionPool;
  }


  @Override
  protected Result check() throws Exception {
    try (StatefulRedisConnection<String, String> connection = this.redisConnectionPool
        .borrowConnection()) {
      if ("PONG".equalsIgnoreCase(connection.sync().ping())) {
        return Result.healthy();
      }
    }
    LOGGER.warn("Can't connect to the database!");
    return Result.unhealthy("Can't connect to the database!");
  }
}
