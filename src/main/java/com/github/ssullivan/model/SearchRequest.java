package com.github.ssullivan.model;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;

public class SearchRequest {
  private GeoRadiusCondition geoRadiusCondition;
  private List<ServicesCondition> conditions;
  private SetOperation finalSetOperation;

  public GeoRadiusCondition getGeoRadiusCondition() {
    return geoRadiusCondition;
  }

  public void setGeoRadiusCondition(GeoRadiusCondition geoRadiusCondition) {
    this.geoRadiusCondition = geoRadiusCondition;
  }

  public void setServiceConditions(final Collection<ServicesCondition> conditions) {
    this.conditions = ImmutableList.copyOf(conditions);
  }

  public List<ServicesCondition> getConditions() {
    return conditions;
  }

  public SetOperation getFinalSetOperation() {
    return finalSetOperation;
  }

  public void setFinalSetOperation(SetOperation finalSetOperation) {
    this.finalSetOperation = finalSetOperation;
  }
}
