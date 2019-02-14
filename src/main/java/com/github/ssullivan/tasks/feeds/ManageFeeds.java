package com.github.ssullivan.tasks.feeds;

import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IndexFacility;
import java.util.Collection;
import java.util.Set;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManageFeeds {

  private static final Logger LOGGER = LoggerFactory.getLogger(ManageFeeds.class);

  private static final long DefaultExpireSeconds = 86400;

  private final IFeedDao feedDao;
  private final IFacilityDao facilityDao;
  private IndexFacility indexDao;

  @Inject
  public ManageFeeds(final IFeedDao feedDao, IFacilityDao facilityDao,
      IndexFacility indexFacility) {
    this.feedDao = feedDao;
    this.facilityDao = facilityDao;
    this.indexDao = indexFacility;
  }

  @Test
  public void expireOldFeeds(final String currentFeedID) throws Exception {
    this.feedDao.setSearchFeedId(currentFeedID);
    this.feedDao.setCurrentFeedId(currentFeedID);

    final Collection<String> feedIds = this.feedDao.getFeedIds();

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

  private boolean expireKeys(final String feedId) {
    try {
      final Set<String> facilityIds = this.facilityDao.getKeysForFeed(feedId);
      for (final String id : facilityIds) {
        this.facilityDao.expire(id, DefaultExpireSeconds);
      }
      this.indexDao.expire(feedId, DefaultExpireSeconds);
      return true;
    } catch (Exception e) {
      LOGGER.error("Failed to expire keys for feed {}", feedId, e);
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
        ;
      }
    }
    return false;
  }
}
