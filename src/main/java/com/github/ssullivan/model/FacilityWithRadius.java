package com.github.ssullivan.model;

public class FacilityWithRadius extends Facility {
  private double radius;



  public FacilityWithRadius(Facility facility, double radius) {
    super(facility);
    this.radius = radius;
  }

  public FacilityWithRadius(double radius) {
    this.radius = radius;
  }


  public double getRadius() {
    return radius;
  }

  public void setRadius(double radius) {
    this.radius = radius;
  }
}
