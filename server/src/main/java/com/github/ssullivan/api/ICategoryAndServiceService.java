package com.github.ssullivan.api;

import com.github.ssullivan.model.SearchResults;
import com.google.inject.ImplementedBy;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@ImplementedBy(ServiceCodesService.class)
public interface ICategoryAndServiceService {

  /**
   * List all of the available SAMSHA service codes.
   *
   * @return a non-null instance of {@link SearchResults}.
   * @throws IOException query failed
   */
  List<String> listServiceCodes() throws IOException;


  /**
   * List all of the available SAMSHA services by category code.
   *
   * @param categoryCode the category code to filter by
   * @return a non-null instance of {@link SearchResults}
   * @throws IOException query failed
   */
  List<String> listServiceCodesInCategory(String categoryCode) throws IOException;

  void loadServiceCodeAndCategories(final InputStream inputStream) throws IOException;
}
