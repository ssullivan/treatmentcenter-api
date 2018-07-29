package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.RedisFacilityDao;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.google.common.collect.ImmutableSet;
import com.google.inject.ImplementedBy;
import io.lettuce.core.GeoArgs;
import java.io.IOException;
import java.util.List;

@ImplementedBy(RedisFacilityDao.class)
public interface IFacilityDao {

  /**
   * Adds the Facility to the database.
   *
   * @param facility
   * @throws IOException
   */
  void addFacility(final Facility facility) throws IOException;

  /**
   * Finds the all of the facilities that any of the specified service codes.
   *
   * @param serviceCodes the SAMSHA service codes
   * @param page control how many results to return
   * @return
   * @throws IOException
   */
  SearchResults<Facility> findByServiceCodes(final List<String> serviceCodes, final Page page)
      throws IOException;

  /**
   * Finds the all of the facilities that any of the specified service codes withhin a certain
   * radius.
   *
   * @param serviceCodes the list of service codes facilities can have
   * @param longitude the longitude coordinate according to WGS84
   * @param latitude the latitude coordinate according to WGS84
   * @param distance radius distance
   * @param geoUnit distance unit (m, km, ft, mi)
   *
   * @return
   */
  SearchResults<Facility> findByServiceCodesWithin(final List<String> serviceCodes,
      double longitude, double latitude, double distance, final String geoUnit, Page page)
      throws IOException;

  SearchResults findByServiceCodes(final ImmutableSet<String> mustServiceCodes, final Page page)
      throws IOException;
}
