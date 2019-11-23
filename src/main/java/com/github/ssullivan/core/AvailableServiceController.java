package com.github.ssullivan.core;

import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.model.AvailableServices;
import com.github.ssullivan.model.Category;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Service;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ServiceManager.Listener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches Categories and Services from redis locally. This will asynchronously refresh cache entries
 * every 30 minutes after write. Additionally, it will pull in new categories and services every 10
 * minutes.
 */
@Singleton
public class AvailableServiceController implements IAvailableServiceController, Managed {

  private static final Logger LOGGER = LoggerFactory.getLogger(AvailableServiceController.class);
  private final ExecutorService pool = Executors.newSingleThreadExecutor();

  private final ICategoryCodesDao categoryCodesDao;
  private final LoadingCache<String, Category> cache;
  private ServiceManager serviceManager;

  /**
   * Creates a new instance of {@link AvailableServiceController}.
   *
   * @param categoryCodesDao the DAO to use for fetching categories
   */
  @Inject
  public AvailableServiceController(final ICategoryCodesDao categoryCodesDao) {
    this.categoryCodesDao = categoryCodesDao;
    this.cache = CacheBuilder.newBuilder()
        .concurrencyLevel(8)
        .maximumSize(256)
        .refreshAfterWrite(30, TimeUnit.MINUTES)
        .expireAfterWrite(3, TimeUnit.DAYS)
        .build(new CategoryCacheLoader(pool, categoryCodesDao));
    this.serviceManager = new ServiceManager(
        Sets.newHashSet(new RefreshCache(this.cache, this.categoryCodesDao)));
    this.serviceManager.addListener(new Listener() {
      @Override
      public void healthy() {
        LOGGER.info("Refresh Cache Service is healthy");
      }

      @Override
      public void stopped() {
        LOGGER.warn("Refresh Cache Service has died");
      }

      @Override
      public void failure(final com.google.common.util.concurrent.Service service) {
        LOGGER.error("Refresh cache Service has failed", service.failureCause());
      }
    });
  }

  @Override
  public List<Facility> applyList(final List<Facility> facilities) {
    facilities.forEach(this::apply);
    return facilities;
  }

  @Override
  public Facility apply(final Facility facility) {
    if (facility == null) {
      return null;
    }
    try {

      // This prevents us from exploding when the cache doesn't contain the categories.
      // It has a couple side effects:
      // (1) We might not get any results (unlikely but possible)
      // (2) We don't force the cache to refresh the cache for this category
      // (3) There is a big performance gain here by not having to reach out to redis
      //     to get the categories. Previously we were having some slowness / deadlocks
      //     with the previous implementation.
      // (4) We mitigate (1), and (2) via a Guava service that refreshes the
      //     cache in bulk every 10 minutes.
      final ImmutableMap<String, Category> cats = this.cache
          .getAllPresent(facility.getCategoryCodes());

      if (cats == null || cats.isEmpty()) {
        LOGGER.warn("Cache Miss for facility {} categories: {}", facility.getId(),
            facility.getCategoryCodes());
        facility.setAvailableServices(new AvailableServices());
      } else {
        // For the available services object we only want the category objects included in it
        // to contains services that the facility actually has
        final Set<Category> facilityCats = cats.values().stream().map(category -> {
          // This contains only the service objects that the facility actually has
          final Set<Service> facilityServices =
              category.getServices().stream()
                  .filter(service -> facility.hasAnyOf(service.getCode()))
                  .collect(
                      Collectors.toSet());

          // Create a new category that only has the services the facility has
          return new Category(category.getCode(), category.getName(), facilityServices);
        }).collect(Collectors.toSet());

        facility.setAvailableServices(new AvailableServices(facilityCats));
      }

      return facility;
    } catch (RuntimeException e) {
      LOGGER.error(
          "An unexpected exception occurred while trying set the available services for facility",
          e);
      return facility;
    }
  }

  private void refreshAll() {
    try {
      LOGGER.info("Refreshing category list from db");
      final List<Category> categoryList = this.categoryCodesDao.listCategories();
      categoryList.forEach(cat -> this.cache.put(cat.getCode(), cat));
      LOGGER.info("Loaded {} cats", categoryList.size());
    } catch (IOException e) {
      LOGGER.error("Failed to list categories!", e);
    }
  }

  @Override
  public void start() throws Exception {
    LOGGER.info("AvailableServiceController is starting");
    this.serviceManager.startAsync();

    refreshAll();
  }

  @Override
  public void stop() throws Exception {
    LOGGER.info("AvailableServiceController is shutting down");
    this.serviceManager.stopAsync();
    this.pool.shutdown();
  }

  /**
   * Periodically refresh the cache from the database.
   */
  private static final class RefreshCache extends AbstractScheduledService implements
      com.google.common.util.concurrent.Service {

    private LoadingCache<String, Category> cache;
    private ICategoryCodesDao dao;

    private RefreshCache(final LoadingCache<String, Category> cache, final ICategoryCodesDao dao) {
      this.cache = cache;
      this.dao = dao;
    }

    @Override
    protected void runOneIteration() throws Exception {
      try {
        LOGGER.info("Refreshing category list from db");
        final List<Category> categoryList = this.dao.listCategories();
        categoryList.forEach(cat -> this.cache.put(cat.getCode(), cat));
        LOGGER.info("Loaded {} cats", categoryList.size());
      } catch (IOException e) {
        LOGGER.error("Failed to list categories!", e);
      }
    }

    @Override
    protected Scheduler scheduler() {
      return Scheduler.newFixedRateSchedule(TimeUnit.MINUTES.toMillis(1),
          TimeUnit.MINUTES.toMillis(10),
          TimeUnit.MILLISECONDS);
    }
  }

  /**
   * CacheLoader that can async refresh the cache
   */
  private static final class CategoryCacheLoader extends CacheLoader<String, Category> {

    private final ListeningExecutorService executorService;
    private final ICategoryCodesDao catsDao;

    private CategoryCacheLoader(final ExecutorService threadPool, final ICategoryCodesDao catsDao) {
      this.executorService = MoreExecutors.listeningDecorator(threadPool);
      this.catsDao = catsDao;
    }

    @Override
    public Category load(final String key) throws Exception {
      Objects.requireNonNull(key, "Category must not be null");
      return this.catsDao.get(key);
    }

    @Override
    public ListenableFuture<Category> reload(final String key, final Category oldValue)
        throws Exception {
      return this.executorService.submit(() -> {
        LOGGER.info("Loading Category information for {}. Old Value was {}", key, oldValue);
        return this.catsDao.get(key);
      });
    }
  }
}
