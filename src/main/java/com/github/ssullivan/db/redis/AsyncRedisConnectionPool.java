package com.github.ssullivan.db.redis;

import com.github.ssullivan.RedisConfig;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.support.AsyncConnectionPoolSupport;
import io.lettuce.core.support.AsyncPool;
import io.lettuce.core.support.BoundedPoolConfig;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncRedisConnectionPool implements IAsyncRedisConnectionPool {
  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncRedisConnectionPool.class);

  private RedisClient redisClient;
  private AsyncPool<StatefulRedisConnection<String, String>> pool;
  private AtomicBoolean isClosed = new AtomicBoolean(false);

  @Inject
  public AsyncRedisConnectionPool(final RedisClient redisClient, final RedisConfig redisConfig) {
    this.redisClient = redisClient;

    this.pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
        () -> redisClient.connectAsync(StringCodec.UTF8, RedisURI.builder()
            .withHost(redisConfig.getHost())
            .withPort(redisConfig.getPort())
            .withDatabase(redisConfig.getDb())
            .build()), BoundedPoolConfig.create());

  }

  public CompletableFuture<StatefulRedisConnection<String, String>> borrowConnection() {
    return this.pool.acquire();
  }

  public <R> CompletableFuture<R> runAsync(Function<RedisAsyncCommands<String, String>, R> function) {
    return borrowConnection()
        .thenCompose(redis -> {
          final RedisAsyncCommands<String, String> async = redis.async();
          return CompletableFuture.completedFuture(function.apply(async))
              .whenComplete((s, throwable) -> pool.release(redis));
        });
  }

  @Override
  public void relase(StatefulRedisConnection<String, String> conn) {
    this.pool.release(conn);
  }

  @Override
  public boolean isClosed() {
    return this.isClosed.get();
  }

  @Override
  public void close() {
    if (isClosed.compareAndSet(false, true)) {
      try {
        this.pool.close();
      } finally {
        this.redisClient.shutdown();
      }
    }
    else {
      LOGGER.warn("Pool has already been closed");
    }
  }

  @Override
  public void closeAsync() {
    if (isClosed.compareAndSet(false, true)) {
      this.pool.closeAsync()
            .whenComplete((s, throwable) -> {
              if (throwable != null) {
                LOGGER.error("Failed to close cleanly", throwable);
              }
              this.redisClient.shutdown();
            });
    }
    else {
      LOGGER.warn("Pool has already been closed");
    }
  }

}
