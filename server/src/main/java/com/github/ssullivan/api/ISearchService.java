package com.github.ssullivan.api;

import com.github.ssullivan.core.FacilitySearchService;
import com.github.ssullivan.model.FacilitySearchQuery;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.google.inject.ImplementedBy;
import java.io.IOException;

@ImplementedBy(FacilitySearchService.class)
public interface ISearchService {

  default SearchResults find(final FacilitySearchQuery facilitySearchQuery) throws IOException {
    return find(facilitySearchQuery, Page.page());
  }

  SearchResults find(final FacilitySearchQuery facilitySearchQuery, final Page page)
      throws IOException;
}
