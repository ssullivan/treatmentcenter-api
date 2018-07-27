package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.RedisFacilityDao;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.google.common.collect.ImmutableSet;
import com.google.inject.ImplementedBy;
import java.io.IOException;
import java.util.List;

@ImplementedBy(RedisFacilityDao.class)
public interface IFacilityDao {

  void addFacility(final Facility facility) throws IOException;

  List<Facility> findByServiceCodes(final List<String> serviceCodes, final Page page)
      throws IOException;

  SearchResults findByServiceCodes(final ImmutableSet<String> mustServiceCodes, final Page page)
      throws IOException;
}
