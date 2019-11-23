package com.github.ssullivan.guice;

import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.db.IManageFeeds;
import com.github.ssullivan.db.redis.RedisFeedManager;
import com.github.ssullivan.healthchecks.RedisHealthCheck;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.lettuce.core.RedisClient;
import javax.annotation.Nonnull;
import javax.inject.Inject;

public class RedisClientModule extends AbstractModule {

  private final RedisConfig redisConfig;

  public RedisClientModule(@Nonnull RedisConfig redisConfig) {
    this.redisConfig = redisConfig;
  }

  @Override
  protected void configure() {
    bind(RedisConfig.class).toInstance(redisConfig);
    bind(RedisClient.class).toProvider(RedisClientProvider.class).in(Singleton.class);
    bind(IManageFeeds.class).to(RedisFeedManager.class);
  }

  @Provides
  @Inject
  IHealthcheckProvider providesDropwizardHealthChecks(final RedisHealthCheck redisHealthCheck) {
    return () -> ImmutableList.of(redisHealthCheck);
  }
}
