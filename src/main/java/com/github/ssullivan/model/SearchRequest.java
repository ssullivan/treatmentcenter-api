package com.github.ssullivan.model;

import com.github.ssullivan.core.analytics.CompositeFacilityScore;
import com.github.ssullivan.core.analytics.Importance;
import com.github.ssullivan.utils.ShortUuid;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;

public class SearchRequest {

  private GeoRadiusCondition geoRadiusCondition;
  private List<ServicesCondition> conditions;
  private SetOperation finalSetOperation;
  private String sortField;
  private SortDirection sortDirection;
  private String id;
  private CompositeFacilityScore compositeFacilityScore;

  public SearchRequest() {
    this.id = ShortUuid.randomShortUuid();
    this.finalSetOperation = SetOperation.INTERSECTION;
    this.sortField = "score";
    this.sortDirection = SortDirection.ASC;

  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getSortField() {
    return sortField;
  }

  public void setSortField(String sortField) {
    this.sortField = sortField;
  }

  public SortDirection getSortDirection() {
    return sortDirection;
  }

  public void setSortDirection(SortDirection sortDirection) {
    this.sortDirection = sortDirection;
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

  public CompositeFacilityScore getCompositeFacilityScore() {
    return compositeFacilityScore;
  }

  public void setCompositeFacilityScore(
      CompositeFacilityScore compositeFacilityScore) {
    this.compositeFacilityScore = compositeFacilityScore;
  }
}
