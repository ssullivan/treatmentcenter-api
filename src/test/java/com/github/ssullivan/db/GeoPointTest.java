package com.github.ssullivan.db;

import com.github.ssullivan.model.GeoPoint;
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
}
