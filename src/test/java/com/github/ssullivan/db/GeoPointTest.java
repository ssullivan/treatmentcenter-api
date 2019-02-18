package com.github.ssullivan.db;

import com.github.ssullivan.model.GeoPoint;
import com.github.ssullivan.model.GeoUnit;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class GeoPointTest {

  @Test
  public void testGetEarthRadiusKilometers() {
    GeoPoint geoPoint = GeoPoint.geoPoint(1, 1);
    final double radius = geoPoint.getEarthRadius("km");

    MatcherAssert.assertThat(radius, Matchers.allOf(Matchers.greaterThanOrEqualTo(6371.0),
        Matchers.lessThanOrEqualTo(6371.1)));
  }

  @Test
  public void testGetEarthRadiusMeters() {
    GeoPoint geoPoint = GeoPoint.geoPoint(1, 1);
    final double radius = geoPoint.getEarthRadius("m");

    MatcherAssert.assertThat(radius, Matchers.allOf(Matchers.greaterThanOrEqualTo(6371000.0),
        Matchers.lessThanOrEqualTo(6371000.1)));
  }

  @Test
  public void testGetUnitMiles() {
    MatcherAssert.assertThat(GeoUnit.asGeoUnit("mi"), Matchers.equalTo(GeoUnit.MILE));
  }

  @Test
  public void testGetUnitKilometers() {
    MatcherAssert.assertThat(GeoUnit.asGeoUnit("km"), Matchers.equalTo(GeoUnit.KILOMETER));
  }

  @Test
  public void testGetUnitMeters() {
    MatcherAssert.assertThat(GeoUnit.asGeoUnit("m"), Matchers.equalTo(GeoUnit.METER));
  }

  @Test
  public void testGetUnitFeet() {
    MatcherAssert.assertThat(GeoUnit.asGeoUnit("ft"), Matchers.equalTo(GeoUnit.FEET));
  }

  @Test
  public void testGetUnitUnknown() {
    MatcherAssert.assertThat(GeoUnit.asGeoUnit("bizz"), Matchers.equalTo(GeoUnit.MILE));
  }
}
