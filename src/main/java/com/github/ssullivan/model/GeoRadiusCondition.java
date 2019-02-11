package com.github.ssullivan.model;

import com.google.common.base.Preconditions;

public class GeoRadiusCondition {

  private GeoPoint geoPoint;
  private double radius;
  private GeoUnit geoUnit;

  public GeoRadiusCondition(GeoPoint geoPoint, double radius,
      GeoUnit geoUnit) {
    this.geoPoint = Preconditions.checkNotNull(geoPoint, "lat, lon must not be null");
    this.radius = Preconditions.checkNotNull(radius, "radius must not be null");
    this.geoUnit = Preconditions.checkNotNull(geoUnit, "radius unit must not be null");
  }

  public GeoRadiusCondition(GeoPoint geoPoint, double radius,
      String geoUnit) {
    this(geoPoint, radius, GeoUnit.asGeoUnit(geoUnit));
  }

  public GeoPoint getGeoPoint() {
    return geoPoint;
  }

  public double getRadius() {
    return radius;
  }

  public GeoUnit getGeoUnit() {
    return geoUnit;
  }
}
