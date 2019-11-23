package com.github.ssullivan.db;

import java.util.Set;

public interface IManageFeeds {

  /**
   * Ensure that the TTL is set to -1 for specified keys
   */
  void persistCriticalIds();

  /**
   * Ensure that the TTL is reset on certain feeds in the advent of an error loading new data
   */
  void bumpExpirationOnSearchFeed();

  /**
   * Ensure that the TTL is set to -1 for the specified keys
   *
   * @param facilityIds a list of ids for facilities
   */
  void persistFacilityIds(Set<String> facilityIds);

  /**
   * Set the TTL for every feed except the currentFeedID. This will update the database and set the
   * search_feed to currentFeedId.
   *
   * @param currentFeedID the current feed that we are loading
   */
  void expireOldFeeds(final String currentFeedID) throws Exception;

  void expireOldFeeds(final String currentFeedID, final long expireSeconds) throws Exception;

  boolean expireKeys(final String feedId, final long expireSeconds);
}
