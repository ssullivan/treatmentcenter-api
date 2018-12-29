package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.RedisCategoryCodesDao;
import com.github.ssullivan.model.Category;
import com.google.inject.ImplementedBy;
import java.io.IOException;
import java.util.List;
import org.eclipse.jetty.util.IO;

@ImplementedBy(RedisCategoryCodesDao.class)
public interface ICategoryCodesDao {

  /**
   * Fetches the service code record from the database with the provided id.
   *
   * @param id the id of the record to fetch
   * @return null if it doesn't exist
   * @throws IOException failed to get from elasticsearch (reasons various)
   */
  Category get(final String id) throws IOException;

  Category get(final String id, final boolean fromCache) throws IOException;

  default Category getFromCache(final String id) throws IOException {
    return get(id, true);
  }

  boolean delete(final String id) throws IOException;

  /**
   * Fetches the service code record from the db.
   *
   * @param categoryCode the service code to retrieve
   * @return null if it doesn't exist
   * @throws IOException failed to query db (reasons various)
   */
  Category getByCategoryCode(final String categoryCode) throws IOException;

  List<String> listCategoryCodes() throws IOException;

  List<Category> listCategories() throws IOException;

  boolean addCategory(final Category category) throws IOException;
}
