package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.index.RedisFacilityIndexDao;
import com.github.ssullivan.model.Facility;
import com.google.inject.ImplementedBy;
import java.io.IOException;

@ImplementedBy(RedisFacilityIndexDao.class)
public interface IndexFacility {
  void index(final String feed, final Facility facility) throws IOException;

  void index(final Facility facility) throws IOException;
}
