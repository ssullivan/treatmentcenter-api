package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.RedisFacilityDao;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.ImplementedBy;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@ImplementedBy(RedisFacilityDao.class)
public interface IFacilityDao {

  ImmutableMap<Long, Facility> fetchAllFacilities() throws IOException;

  long getLargestPrimaryKey() throws IOException;

  /**
   * Adds the Facility to the database.
   */
  void addFacility(final Facility facility) throws IOException;

  void updateFacility(final Facility facility) throws IOException;

  Facility getFacility(final String pk) throws IOException;

  default Facility getFacility(final Long pk) throws IOException {
    return getFacility(Objects.requireNonNull("" + pk, "Facility primary key must not be null"));
  }

  /**
   * Finds the all of the facilities that any of the specified service codes.
   *
   * @param serviceCodes the SAMSHA service codes
   * @param page control how many results to return
   */
  SearchResults<Facility> findByServiceCodes(final List<String> serviceCodes, final Page page)
      throws IOException;

  SearchResults findByServiceCodes(final ImmutableSet<String> mustServiceCodes, final Page page)
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
   */
  SearchResults<Facility> findByServiceCodesWithin(final List<String> serviceCodes,
      double longitude, double latitude, double distance, final String geoUnit, Page page)
      throws IOException;
}
