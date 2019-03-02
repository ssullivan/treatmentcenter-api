package com.github.ssullivan.db.postgres;

import com.github.ssullivan.core.AvailableServiceController.CategoryCacheLoader;
import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.model.Category;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.dropwizard.lifecycle.Managed;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceCodeLookupCache implements IServiceCodeLookupCache, Managed {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceCodeLookupCache.class);

  private final ExecutorService pool = Executors.newSingleThreadExecutor();
  private final LoadingCache<String, Integer> cache;

  @Inject
  public ServiceCodeLookupCache(final DSLContext dslContext) {
    this.cache =  CacheBuilder.newBuilder()
        .concurrencyLevel(8)
        .maximumSize(256)
        .refreshAfterWrite(30, TimeUnit.MINUTES)
        .expireAfterWrite(3, TimeUnit.DAYS)
        .build(new ServiceIdCacheLoader(pool, dslContext));
  }


  @Override
  public void start() throws Exception {

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
      return this.dsl.select(Tables.SERVICE.ID)
          .from(Tables.SERVICE)
          .where(Tables.SERVICE.CODE.eq(key))
          .fetch(Tables.SERVICE.ID)
          .stream()
          .findFirst()
          .orElse(null);
    }


    @Override
    public ListenableFuture<Integer> reload(final String key, final Integer oldValue) throws Exception {
      return this.executorService.submit(() -> load(key));
    }
  }
}
