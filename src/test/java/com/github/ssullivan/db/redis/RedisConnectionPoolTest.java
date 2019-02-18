package com.github.ssullivan.db.redis;

import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.guice.RedisClientModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RedisConnectionPoolTest {

  @Test
  public void testClosingPool() throws Exception {
    final RedisConfig redisConfig = new RedisConfig("127.0.0.1", 6379, 1);
    redisConfig.setTimeout(50);

    final Injector injector = Guice
        .createInjector(new RedisClientModule(redisConfig));

    IRedisConnectionPool redisConnectionPool = injector.getInstance(IRedisConnectionPool.class);
    redisConnectionPool.close();
    MatcherAssert.assertThat(redisConnectionPool.isClosed(), Matchers.equalTo(true));
    Assertions.assertThrows(IllegalStateException.class, redisConnectionPool::borrowConnection,
        "Pool is closed");

  }

}
