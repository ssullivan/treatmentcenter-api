package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.index.RedisIndexFacilityByServiceCode;
import com.github.ssullivan.model.Facility;
import com.google.inject.ImplementedBy;
import com.google.inject.Singleton;
import java.io.IOException;

@ImplementedBy(RedisIndexFacilityByServiceCode.class)
public interface IndexFacilityByServiceCode extends IndexFacility {

  void index(final String feed, final Facility facility) throws IOException;
}
