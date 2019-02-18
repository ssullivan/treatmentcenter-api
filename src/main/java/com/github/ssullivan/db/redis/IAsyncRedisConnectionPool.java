package com.github.ssullivan.db.redis;

import com.google.inject.ImplementedBy;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@ImplementedBy(AsyncRedisConnectionPool.class)
public interface IAsyncRedisConnectionPool {

  CompletableFuture<StatefulRedisConnection<String, String>> borrowConnection();

  <R> CompletionStage<R> runAsync(Function<RedisAsyncCommands<String, String>, R> function);

  void relase(StatefulRedisConnection<String, String> conn);

  boolean isClosed();

  void close();

  void closeAsync();
}
