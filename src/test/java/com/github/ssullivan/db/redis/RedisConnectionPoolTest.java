package com.github.ssullivan.db.redis;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.lettuce.core.RedisClient;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class RedisConnectionPoolTest {

  @Test
  public void testClosingPool() throws Exception {

    final Injector injector = Guice
        .createInjector(new AbstractModule() {
          @Override
          protected void configure() {
            bind(RedisClient.class).toInstance(Mockito.mock(RedisClient.class));
          }
        });

    IRedisConnectionPool redisConnectionPool = injector.getInstance(IRedisConnectionPool.class);
    redisConnectionPool.close();
    MatcherAssert.assertThat(redisConnectionPool.isClosed(), Matchers.equalTo(true));
    Assertions.assertThrows(IllegalStateException.class, redisConnectionPool::borrowConnection,
        "Pool is closed");

  }

}
