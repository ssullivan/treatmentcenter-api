package com.github.ssullivan.core;

import com.github.ssullivan.api.ISearchService;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.model.FacilitySearchQuery;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import java.io.IOException;
import javax.inject.Inject;

public class FacilitySearchService implements ISearchService {

  private final IFacilityDao facilityDao;

  @Inject
  FacilitySearchService(IFacilityDao facilityDao) {
    this.facilityDao = facilityDao;
  }

  @Override
  public SearchResults find(FacilitySearchQuery facilitySearchQuery, final Page page)
      throws IOException {
    return this.facilityDao.findByServiceCodes(facilitySearchQuery.serviceCodes(), page);
  }
}
