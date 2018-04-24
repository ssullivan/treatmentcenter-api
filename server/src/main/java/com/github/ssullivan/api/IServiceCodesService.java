package com.github.ssullivan.api;

import com.github.ssullivan.core.ServiceCodesService;
import com.github.ssullivan.model.SearchResults;
import com.google.inject.ImplementedBy;
import java.io.IOException;

@ImplementedBy(ServiceCodesService.class)
public interface IServiceCodesService {

  /**
   * List all of the available SAMSHA service codes.
   *
   * @return a non-null instance of {@link SearchResults}.
   * @throws IOException query failed
   */
  SearchResults listServiceCodes() throws IOException;


  /**
   * List all of the available SAMSHA services by category code.
   *
   * @param categoryCode the category code to filter by
   * @return a non-null instance of {@link SearchResults}
   * @throws IOException query failed
   */
  SearchResults listServiceCodesInCategory(String categoryCode) throws IOException;
}
