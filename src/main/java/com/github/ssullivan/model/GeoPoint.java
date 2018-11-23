package com.github.ssullivan.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class GeoPoint {
  private static final double EARTH_RADIUS_KM = 6_371.0;

  @JsonCreator
  public static GeoPoint geoPoint(@JsonProperty("lat") final double lat,
      @JsonProperty("lon") final double lon) {
    return new AutoValue_GeoPoint(lat, lon);
  }

  @JsonProperty("lat")
  public abstract double lat();

  @JsonProperty("long")
  public abstract double lon();

  public static boolean isValidLat(final double lat) {
    return !(lat > 90) && !(lat < -90);
  }

  public static boolean isValidLon(final double lon) {
    return !(lon > 180) && !(lon < -180);
  }

  public static boolean isValidLatLong(final double lat, final double lon) {
    return isValidLat(lat) && isValidLon(lon);
  }

  public double getDistance(final GeoPoint other, final String geoUnit) {
    return distanceInRadians(other) * getEarthRadius(geoUnit);
  }

  public double getEarthRadius(final String geoUnit) {
    switch (geoUnit) {
      case "m":
        return GeoUnit.KILOMETER.convertTo(GeoUnit.METER, EARTH_RADIUS_KM);
      case "km":
        return EARTH_RADIUS_KM;
      case "ft":
        return GeoUnit.KILOMETER.convertTo(GeoUnit.FEET, EARTH_RADIUS_KM);
      case "mi":
        return GeoUnit.KILOMETER.convertTo(GeoUnit.MILE, EARTH_RADIUS_KM);
      default:
        throw new IllegalArgumentException("Invalid geoUnit: " + geoUnit);
    }
  }

  /**
   * Calculate the distance in radians between two lat, long points
   *
   * @param rhs the other point
   * @return the distance in radians
   */
  public double distanceInRadians(final GeoPoint rhs) {
    final double lat1Radians = Math.toRadians(lat());
    final double lat2Radians = Math.toRadians(rhs.lat());

    final double deltaLatRadians = Math.abs(lat2Radians - lat1Radians);
    final double deltaLonRadians =
        Math.abs(Math.toRadians(rhs.lon() - lon()));

    double temp = Math.sin(deltaLatRadians / 2) *
        Math.sin(deltaLatRadians / 2) +
        Math.cos(lat1Radians) * Math.cos(lat2Radians)
        * Math.sin(deltaLonRadians / 2) *
            Math.sin(deltaLonRadians / 2);

    return 2 * Math.atan2(Math.sqrt(temp), Math.sqrt(1 - temp));
  }




}
