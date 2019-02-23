package com.github.ssullivan.tasks.feeds;

import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IndexFacility;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.db.redis.RedisCategoryCodesDao;
import com.github.ssullivan.db.redis.RedisFeedDao;
import com.github.ssullivan.db.redis.RedisServiceCodeDao;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManageFeeds {

  private static final Logger LOGGER = LoggerFactory.getLogger(ManageFeeds.class);

  private static final long DefaultExpireSeconds = TimeUnit.HOURS.toSeconds(12);

  private final IFeedDao feedDao;
  private final IFacilityDao facilityDao;
  private IndexFacility indexDao;
  private IRedisConnectionPool redisConnectionPool;

  @Inject
  public ManageFeeds(final IFeedDao feedDao, IFacilityDao facilityDao,
      IndexFacility indexFacility,
      IRedisConnectionPool pool) {
    this.feedDao = feedDao;
    this.facilityDao = facilityDao;
    this.indexDao = indexFacility;
    this.redisConnectionPool = pool;
  }


  public void expireOldFeeds(final String currentFeedID) throws Exception {
    this.feedDao.setSearchFeedId(currentFeedID);
    this.feedDao.setCurrentFeedId(currentFeedID);

    final Collection<String> feedIds = this.feedDao.getFeedIds();
    if (feedIds.isEmpty()) {
      LOGGER.warn("There were no known feed ids to clear!");
    } else {
      try {
        for (final String feedId : feedIds) {
          if (!feedId.equalsIgnoreCase(currentFeedID)) {
            if (expireKeys(feedId)) {
              LOGGER.info("Set expiration for all keys for feed {}", feedId);
            } else {
              LOGGER.info("Set expiration for all some or all keys for feed {} failed!", feedId);
            }
          }
        }
      }
      finally {
        // ensure the following keys dont have a TTL set
        try (StatefulRedisConnection<String, String> conn = redisConnectionPool.borrowConnection()) {
          final RedisCommands<String, String> sync = conn.sync();
          sync.persist(RedisCategoryCodesDao.KEY);
          sync.persist(RedisServiceCodeDao.KEY);
          sync.persist(RedisFeedDao.CURRENT_FEED_KEY);
          sync.persist(RedisFeedDao.FEED_IDS_KEY);
          sync.persist(RedisFeedDao.SEARCH_FEED_KEY);
        }
      }
    }
  }

  private boolean expireKeys(final String feedId) {
    try {
      // This will set a TTL for every location that was loaded for this feed id
      // If there is an existing TTL it will not overwrite it
      this.facilityDao.expire(feedId, DefaultExpireSeconds, false);

      // This
      this.indexDao.expire(feedId, DefaultExpireSeconds, false);
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
