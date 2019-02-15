package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.RedisServiceCodeDao;
import com.github.ssullivan.model.Service;
import com.google.inject.ImplementedBy;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;

@ImplementedBy(RedisServiceCodeDao.class)
public interface IServiceCodesDao {

  /**
   * Fetches the service code record from the database with the provided id.
   *
   * @param id the id of the record to fetch
   * @return null if it doesn't exist
   * @throws IOException failed to get from elasticsearch (reasons various)
   */
  Service get(final String id) throws IOException;

  Service get(final String id, final boolean fromCache) throws IOException;

  default Service getFromCache(final String id) throws IOException {
    return get(id, true);
  }

  boolean delete(final String id) throws IOException;

  /**
   * Fetches the service code record from the db.
   *
   * @param serviceCode the service code to retrieve
   * @return null if it doesn't exist
   * @throws IOException failed to query elasticsearch (reasons various)
   */
  Service getByServiceCode(final String serviceCode) throws IOException;

  List<Service> listServices() throws IOException;

  List<String> listServiceCodes() throws IOException;

  List<String> listServiceCodesInCategory(final String category) throws IOException;

  boolean addService(final Service service) throws IOException;

  boolean addService(final String feedId, final Service service) throws IOException;
}
