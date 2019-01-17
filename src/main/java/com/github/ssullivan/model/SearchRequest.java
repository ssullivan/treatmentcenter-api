package com.github.ssullivan.model;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;

public class SearchRequest {
  private GeoRadiusCondition geoRadiusCondition;
  private List<ServicesCondition> conditions;
  private ServicesCondition firstCondition;
  private ServicesCondition secondCondition;
  private ServicesCondition mustNotCondition;

  public GeoRadiusCondition getGeoRadiusCondition() {
    return geoRadiusCondition;
  }

  public void setGeoRadiusCondition(GeoRadiusCondition geoRadiusCondition) {
    this.geoRadiusCondition = geoRadiusCondition;
  }

  @Deprecated
  public ServicesCondition getFirstCondition() {
    return firstCondition;
  }

  @Deprecated
  public void setFirstCondition(ServicesCondition firstCondition) {
    this.firstCondition = firstCondition;
  }

  @Deprecated
  public ServicesCondition getSecondCondition() {
    return secondCondition;
  }

  @Deprecated
  public void setSecondCondition(ServicesCondition secondCondition) {
    this.secondCondition = secondCondition;
  }

  @Deprecated
  public ServicesCondition getMustNotCondition() {
    return mustNotCondition;
  }

  @Deprecated
  public void setMustNotCondition(ServicesCondition mustNotCondition) {
    this.mustNotCondition = mustNotCondition;
  }

  public void setServiceConditions(final Collection<ServicesCondition> conditions) {
    this.conditions = ImmutableList.copyOf(conditions);
  }

  public List<ServicesCondition> getConditions() {
    return conditions;
  }
}
