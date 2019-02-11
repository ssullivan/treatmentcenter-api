package com.github.ssullivan.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Objects;

@ApiModel
public class FacilityWithRadius extends Facility {

  private double radius;
  private String geoUnit;

  public FacilityWithRadius() {
  }

  public FacilityWithRadius(Facility facility, double radius) {
    super(facility);
    this.radius = radius;
  }

  public FacilityWithRadius(Facility facility, double radius, String geoUnit) {
    super(facility);
    this.radius = radius;
    this.geoUnit = geoUnit;
  }

  public FacilityWithRadius(double radius) {
    this.radius = radius;
  }


  @ApiModelProperty("the distance from the search lat, lon")
  public double getRadius() {
    return radius;
  }

  public void setRadius(double radius) {
    this.radius = radius;
  }


  @ApiModelProperty("the unit that the distance is in")
  public String getGeoUnit() {
    return geoUnit;
  }

  public void setGeoUnit(String geoUnit) {
    this.geoUnit = geoUnit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FacilityWithRadius that = (FacilityWithRadius) o;
    return Double.compare(that.radius, radius) == 0 &&
        Objects.equals(geoUnit, that.geoUnit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(radius, geoUnit);
  }
}
