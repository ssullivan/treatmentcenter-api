package com.github.ssullivan.db.redis;

import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.FacilityWithRadius;
import com.github.ssullivan.model.GeoPoint;
import java.util.function.Function;

public class ToFacilityWithRadiusConverter implements Function<Facility, FacilityWithRadius> {
  private final double latitude;
  private final double longitude;
  private final String geoUnit;

  public ToFacilityWithRadiusConverter(final double latitude, final double longitude, final String geoUnit) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.geoUnit = geoUnit;
  }

  @Override
  public FacilityWithRadius apply(final Facility facility) {
    if (facility.getLocation() != null) {

      return new FacilityWithRadius(facility, facility.getLocation()
          .getDistance(GeoPoint.geoPoint(latitude, longitude), geoUnit),
          geoUnit);
    }

    return new FacilityWithRadius(facility, 0.0, geoUnit);
  }
}
