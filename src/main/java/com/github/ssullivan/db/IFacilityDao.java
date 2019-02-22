package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.RedisFacilityDao;
import com.github.ssullivan.model.Facility;
import com.google.inject.ImplementedBy;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

@ImplementedBy(RedisFacilityDao.class)
public interface IFacilityDao {

  /**
   * Adds the Facility to the database.
   */
  void addFacility(final String feedId, final Facility facility) throws IOException;

  void addFacility(final String feedId, final List<Facility> facility) throws IOException;

  Facility getFacility(final String id) throws IOException;

  List<Facility> fetchBatch(final Collection<String> ids);

  CompletionStage<List<Facility>> fetchBatchAsync(final Collection<String> ids);

  Set<String> getKeysForFeed(final String feedId) throws IOException;

  Boolean expire(final String id, long seconds) throws IOException;

  /**
   * Expires all keys associated with this feed for locations
   *
   * @param feed the feed to expire
   * @param seconds the seconds to set the ttl too
   * @param overwrite if true, if the key already has a ttl it will reset the ttl
   * @return true success / false failure
   * @throws IOException
   */
  Boolean expire(final String feed, long seconds, boolean overwrite) throws IOException;

}
