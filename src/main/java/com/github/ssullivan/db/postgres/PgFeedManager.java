package com.github.ssullivan.db.postgres;

import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IManageFeeds;
import java.util.Set;
import javax.inject.Inject;

public class PgFeedManager implements IManageFeeds {

  private IFeedDao feedDao;

  @Inject
  public PgFeedManager(IFeedDao feedDao) {
    this.feedDao = feedDao;
  }

  @Override
  public void persistCriticalIds() {

  }

  @Override
  public void bumpExpirationOnSearchFeed() {

  }

  @Override
  public void persistFacilityIds(final Set<String> facilityIds) {

  }

  @Override
  public void expireOldFeeds(final String currentFeedID) throws Exception {
    this.feedDao.setSearchFeedId(currentFeedID);
  }

  @Override
  public void expireOldFeeds(final String currentFeedID, final long expireSeconds)
      throws Exception {
    this.feedDao.setSearchFeedId(currentFeedID);
  }

  @Override
  public boolean expireKeys(final String feedId, final long expireSeconds) {
    return true;
  }
}
