package com.github.ssullivan.model;

public class GeoRadiusCondition {
  private GeoPoint geoPoint;
  private double radius;
  private GeoUnit geoUnit;

  public GeoRadiusCondition(GeoPoint geoPoint, double radius,
      GeoUnit geoUnit) {
    this.geoPoint = geoPoint;
    this.radius = radius;
    this.geoUnit = geoUnit;
  }

  public GeoRadiusCondition(GeoPoint geoPoint, double radius,
      String geoUnit) {
    this(geoPoint, radius, GeoUnit.valueOf(geoUnit));
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
