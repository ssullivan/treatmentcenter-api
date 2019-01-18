package com.github.ssullivan.guice;

import com.github.ssullivan.RedisConfig;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import java.time.Duration;
import javax.inject.Inject;
import javax.inject.Provider;

public class RedisClientProvider implements Provider<RedisClient> {
  private final RedisConfig redisConfig;

  @Inject
  public RedisClientProvider(final RedisConfig redisConfig) {
    this.redisConfig = redisConfig;
  }

  @Override
  public RedisClient get() {
    return RedisClient.create(RedisURI
        .builder()
        .withHost(redisConfig.getHost())
        .withPort(redisConfig.getPort())
        .withDatabase(redisConfig.getDb())
        .withTimeout(Duration.ofSeconds(60))
        .build());
  }
}
