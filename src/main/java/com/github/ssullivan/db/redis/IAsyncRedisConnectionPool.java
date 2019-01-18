package com.github.ssullivan.db.redis;

import com.google.inject.ImplementedBy;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@ImplementedBy(AsyncRedisConnectionPool.class)
public interface IAsyncRedisConnectionPool {

  CompletableFuture<StatefulRedisConnection<String, String>> borrowConnection();

  <R> CompletableFuture<R> runAsync(Function<RedisAsyncCommands<String, String>, R> function);

  boolean isClosed();

  void close();

  void closeAsync();
}
