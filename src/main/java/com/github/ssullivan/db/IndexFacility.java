package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.index.RedisFacilityIndexDao;
import com.github.ssullivan.model.Facility;
import com.google.inject.ImplementedBy;
import java.io.IOException;
import java.util.List;

@ImplementedBy(RedisFacilityIndexDao.class)
public interface IndexFacility {

  void index(final String feed, final Facility facility) throws IOException;

  void index(final String feed, final List<Facility> batch) throws IOException;

  void index(final Facility facility) throws IOException;

  void expire(final String feed, final long seconds, boolean overwrite) throws Exception;
}
