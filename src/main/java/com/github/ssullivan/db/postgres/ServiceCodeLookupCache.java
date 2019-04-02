package com.github.ssullivan.db.postgres;

import com.github.ssullivan.db.psql.Tables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.*;
import io.dropwizard.lifecycle.Managed;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class ServiceCodeLookupCache implements IServiceCodeLookupCache, Managed {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceCodeLookupCache.class);

  private final ExecutorService pool = Executors.newSingleThreadExecutor();
  private final LoadingCache<String, Integer> cache;
  private final DSLContext dsl;

  @Inject
  public ServiceCodeLookupCache(final DSLContext dslContext) {
    this.dsl = dslContext;
    this.cache =  CacheBuilder.newBuilder()
        .concurrencyLevel(8)
        .maximumSize(512)
        .refreshAfterWrite(30, TimeUnit.MINUTES)
        .expireAfterWrite(3, TimeUnit.DAYS)
        .build(new ServiceIdCacheLoader(pool, dslContext));
  }

  public Integer lookup(final String serviceCode) throws ExecutionException {
    Objects.requireNonNull(serviceCode, "Service code must not be null");
    try {
      return this.cache.get(serviceCode);
    } catch (ExecutionException | UncheckedExecutionException | ExecutionError e) {
      LOGGER.error("Failed to load value from cache for key '{}'", serviceCode, e);
      throw e;
    }
  }

  @Override
  public Set<Integer> lookupSet(Collection<String> serviceCodes) {
    return serviceCodes.stream().map(it -> {
      try {
        return lookup(it);
      }
      catch (ExecutionException | UncheckedExecutionException | ExecutionError e) {
        LOGGER.error("Failed to lookup service code '{}'", it, e);
      }
      return null;
    }).filter(Objects::nonNull)
            .collect(Collectors.toSet());
  }


  @Override
  public void start() throws Exception {
    try {
      this.cache.putAll(this.dsl.select(Tables.SERVICE.CODE, Tables.SERVICE.ID)
              .from(Tables.SERVICE)
              .fetchMap(Tables.SERVICE.CODE, Tables.SERVICE.ID));
    }
    catch (DataAccessException e) {
      LOGGER.error("Failed to fetch all service codes from database", e);
    }
  }

  @Override
  public void stop() throws Exception {
    this.pool.shutdown();
  }


  /**
   * CacheLoader that can async refresh the cache
   */
  private static final class ServiceIdCacheLoader extends CacheLoader<String, Integer> {
    private final ListeningExecutorService executorService;
    private final DSLContext dsl;

    private ServiceIdCacheLoader(final ExecutorService threadPool, final DSLContext dsl) {
      this.executorService = MoreExecutors.listeningDecorator(threadPool);
      this.dsl = dsl;
    }

    @Override
    public Integer load(final String key) throws Exception {
      try {
        return this.dsl.select(Tables.SERVICE.ID)
                .from(Tables.SERVICE)
                .where(Tables.SERVICE.CODE.eq(key))
                .fetch(Tables.SERVICE.ID)
                .stream()
                .findFirst()
                .orElse(null);
      }
      catch (DataAccessException e) {
        LOGGER.error("Failed to get service code {}", key, e);
        return null;
      }
    }


    @Override
    public ListenableFuture<Integer> reload(final String key, final Integer oldValue) throws Exception {
      return this.executorService.submit(() -> load(key));
    }
  }
}
