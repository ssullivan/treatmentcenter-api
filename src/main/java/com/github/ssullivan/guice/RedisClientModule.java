package com.github.ssullivan.guice;

import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.db.redis.RedisConnectionPool;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.lettuce.core.RedisClient;
import javax.annotation.Nonnull;

public class RedisClientModule extends AbstractModule {
  private final RedisConfig redisConfig;

  public RedisClientModule(@Nonnull RedisConfig redisConfig) {
    this.redisConfig = redisConfig;
  }

  @Override
  protected void configure() {
    bind(RedisConfig.class).toInstance(redisConfig);
    bind(RedisClient.class).toProvider(RedisClientProvider.class).in(Singleton.class);
    bind(IRedisConnectionPool.class).to(RedisConnectionPool.class).in(Singleton.class);
  }



}
