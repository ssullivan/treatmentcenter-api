package com.github.ssullivan.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class GeoPoint {

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
}
