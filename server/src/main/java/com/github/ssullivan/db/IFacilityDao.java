package com.github.ssullivan.db;

import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.google.common.collect.ImmutableSet;
import com.google.inject.ImplementedBy;
import java.io.IOException;

@ImplementedBy(ElasticFacilityDao.class)
public interface IFacilityDao {

  SearchResults findByServiceCodes(final ImmutableSet<String> mustServiceCodes, final Page page)
      throws IOException;
}
