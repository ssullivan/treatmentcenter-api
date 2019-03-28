package com.github.ssullivan.db.redis;

import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IManageFeeds;
import com.github.ssullivan.db.IndexFacility;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisFeedManager implements IManageFeeds {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisFeedManager.class);

  private static final long DefaultExpireSeconds = TimeUnit.DAYS.toSeconds(2);

  private final IFeedDao feedDao;
  private final IFacilityDao facilityDao;
  private IndexFacility indexDao;
  private IRedisConnectionPool redisConnectionPool;

  @Inject
  public RedisFeedManager(final IFeedDao feedDao, IFacilityDao facilityDao,
                          IndexFacility indexFacility,
                          IRedisConnectionPool pool) {
    this.feedDao = feedDao;
    this.facilityDao = facilityDao;
    this.indexDao = indexFacility;
    this.redisConnectionPool = pool;
  }

  @Override
  public void persistCriticalIds() {
    // ensure the following keys dont have a TTL set
    try (StatefulRedisConnection<String, String> conn = redisConnectionPool.borrowConnection()) {
      final RedisCommands<String, String> sync = conn.sync();
      sync.persist(RedisCategoryCodesDao.KEY);
      sync.persist(RedisServiceCodeDao.KEY);
      sync.persist(RedisFeedDao.CURRENT_FEED_KEY);
      sync.persist(RedisFeedDao.FEED_IDS_KEY);
      sync.persist(RedisFeedDao.SEARCH_FEED_KEY);
    } catch (Exception e) {
      LOGGER.error("Failed to persist crit ids", e);
    }
  }

  @Override
  public void bumpExpirationOnSearchFeed() {

    try {
      this.feedDao.searchFeedId()
          .ifPresent(id -> {
            LOGGER.info("Attempting to bump expiration for keys associated with: {}", id);
            expireKeys(id, DefaultExpireSeconds);
          });
    } catch (IOException e) {
      LOGGER.error("Failed to bump expiration on keys for current/original search feed", e);
    }

  }

  @Override
  public void persistFacilityIds(Set<String> facilityIds) {
    // ensure the following keys dont have a TTL set
    try (StatefulRedisConnection<String, String> conn = redisConnectionPool.borrowConnection()) {
      final RedisCommands<String, String> sync = conn.sync();

      for (final String facilityId : facilityIds) {
        int retries = 3;
        do {
          try {
            sync.persist(RedisConstants.TREATMENT_FACILITIES + ":" + facilityId);
            LOGGER.info("PERSIST {}:{}", RedisConstants.TREATMENT_FACILITIES, facilityId);
            break;
          }
          catch (Exception e) {
            LOGGER.error("Failed to persist {}", facilityId, e);
          }
        } while (retries-- > 0);
      }

    } catch (Exception e) {
      LOGGER.error("Failed to persist all facility ids", e);
    }
  }

  @Override
  public void expireOldFeeds(final String currentFeedID) throws Exception {
    expireOldFeeds(currentFeedID, DefaultExpireSeconds);
  }

  @Override
  public void expireOldFeeds(final String currentFeedID, final long expireSeconds) throws Exception {
    LOGGER.info("expireOldFeeds currentFeedId is {}, {}", currentFeedID, expireSeconds);
    Stopwatch stopwatch = Stopwatch.createStarted();

    // This is the id of the feed that the API is currently searching
    Optional<String> originalSearchFeedId = this.feedDao.searchFeedId();
    originalSearchFeedId.ifPresent(s -> LOGGER.info("Original Search Feed is {}", s));

    // Try and point the API to the new data we just loaded
    Optional<String> searchFeedIdOption = this.feedDao.setSearchFeedId(currentFeedID);

    // If we failed to update the pointer then we don't want to expire the old data (if possible)
    if (!searchFeedIdOption.isPresent()) {
      LOGGER.error("Failed to set search feed id to {}", currentFeedID);
      originalSearchFeedId.ifPresent(it -> expireKeys(it, DefaultExpireSeconds));
      persistCriticalIds();

      return;
    }
    else {
      LOGGER.info("Set search feed id to {} [this is our new data]", currentFeedID);
    }

    this.feedDao.setCurrentFeedId(currentFeedID);

    final Collection<String> feedIds = this.feedDao.getFeedIds();
    if (feedIds.isEmpty()) {
      LOGGER.warn("There were no known feed ids to clear!");
    } else {
      try {
        for (final String feedId : feedIds) {
          if (!feedId.equalsIgnoreCase(currentFeedID)) {
            if (expireKeys(feedId, expireSeconds)) {
              LOGGER.info("Set expiration for all keys for feed {}", feedId);
            } else {
              LOGGER.info("Set expiration for all some or all keys for feed {} failed!", feedId);
            }
          }
        }
      }
      finally {
        persistCriticalIds();
        this.feedDao.setSearchFeedId(currentFeedID);
      }
      LOGGER.info("Finished expiring old feeds after {}", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
  }

  @Override
  public boolean expireKeys(final String feedId, final long expireSeconds) {
    try {
      // This will set a TTL for every location that was loaded for this feed id
      // If there is an existing TTL it will not overwrite it
      this.facilityDao.expire(feedId, expireSeconds, false);

      // This
      this.indexDao.expire(feedId, expireSeconds, false);
      this.feedDao.removeFeedId(feedId);
      return true;
    } catch (Exception e) {
      LOGGER.error("Failed to expire keys for feed {}", feedId, e);
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
    }
    return false;
  }
}
