package com.github.ssullivan.model;

public class SearchRequest {
  private GeoRadiusCondition geoRadiusCondition;
  private ServicesCondition firstCondition;
  private ServicesCondition secondCondition;
  private ServicesCondition mustNotCondition;

  public GeoRadiusCondition getGeoRadiusCondition() {
    return geoRadiusCondition;
  }

  public void setGeoRadiusCondition(GeoRadiusCondition geoRadiusCondition) {
    this.geoRadiusCondition = geoRadiusCondition;
  }

  public ServicesCondition getFirstCondition() {
    return firstCondition;
  }

  public void setFirstCondition(ServicesCondition firstCondition) {
    this.firstCondition = firstCondition;
  }

  public ServicesCondition getSecondCondition() {
    return secondCondition;
  }

  public void setSecondCondition(ServicesCondition secondCondition) {
    this.secondCondition = secondCondition;
  }

  public ServicesCondition getMustNotCondition() {
    return mustNotCondition;
  }

  public void setMustNotCondition(ServicesCondition mustNotCondition) {
    this.mustNotCondition = mustNotCondition;
  }
}
