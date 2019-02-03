package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.RedisFacilityDao;
import com.github.ssullivan.model.AvailableServices;
import com.github.ssullivan.model.Facility;
import com.google.inject.ImplementedBy;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;

@ImplementedBy(RedisFacilityDao.class)
public interface IFacilityDao {

  /**
   * Adds the Facility to the database.
   */
  void addFacility(final Facility facility) throws IOException;

  Facility getFacility(final String id) throws IOException;

  List<Facility> fetchBatch(final Collection<String> ids);

  CompletionStage<List<Facility>> fetchBatchAsync(final Collection<String> ids);

  AvailableServices getAvailableServices( final Facility facility);

}
