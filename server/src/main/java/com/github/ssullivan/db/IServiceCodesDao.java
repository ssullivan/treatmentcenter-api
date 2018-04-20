package com.github.ssullivan.db;

import com.github.ssullivan.model.SearchResults;
import com.google.inject.ImplementedBy;
import java.io.IOException;
import java.util.Map;

@ImplementedBy(ElasticServiceCodesDao.class)
public interface IServiceCodesDao {

  /**
   * Fetches the service code record from elasticsearch with the provided id.
   *
   * @param id the id of the record to fetch
   * @return null if it doesn't exist
   * @throws IOException failed to get from elasticsearch (reasons various)
   */
  Map<String, Object> get(final String id) throws IOException;

  /**
   * Fetches the service code record from elasticsearch.
   *
   * @param serviceCode the service code to retrieve
   * @return null if it doesn't exist
   * @throws IOException failed to query elasticsearch (reasons various)
   */
  Map<String, Object> getByServiceCode(final String serviceCode) throws IOException;

  SearchResults listServiceCodes() throws IOException;

  SearchResults listServiceCodesInCategory(final String category) throws IOException;

}
