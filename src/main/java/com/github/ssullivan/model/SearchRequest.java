package com.github.ssullivan.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.util.Collection;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

public class SearchRequest {
  private GeoRadiusCondition geoRadiusCondition;
  private List<ServicesCondition> conditions;
  private SetOperation finalSetOperation;

  public SearchRequest() {
    this.finalSetOperation = SetOperation.INTERSECTION;
  }

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
    if (this.conditions == null) {
      return ImmutableList.of();
    }
    return conditions;
  }

  public SetOperation getFinalSetOperation() {
    return finalSetOperation;
  }

  public void setFinalSetOperation(SetOperation finalSetOperation) {
    this.finalSetOperation = finalSetOperation;
  }

  public ImmutableSet<String> allServiceCodes() {
    if (this.conditions == null || this.conditions.isEmpty()) {
      return ImmutableSet.of();
    }
    final ImmutableSet.Builder<String> builder = new Builder<>();
    this.conditions
        .stream()
        .filter(it -> it.getMatchOperator() != MatchOperator.MUST_NOT)
        .flatMap(it -> it.getServices().stream())
        .forEach(builder::add);
    return builder.build();
  }
}
