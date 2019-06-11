package com.github.ssullivan.model;

import com.github.ssullivan.model.conditions.RangeCondition;
import org.w3c.dom.ranges.Range;

public class RecoveryHousingSearchRequest {
  private String city;
  private String zipcode;
  private RangeCondition capacity;

  public RecoveryHousingSearchRequest(String city, String zipcode,
      RangeCondition capacity) {
    this.city = city;
    this.zipcode = zipcode;
    this.capacity = capacity;
  }

  public RecoveryHousingSearchRequest() {
  }

  public String getCity() {
    return city;
  }

  public String getZipcode() {
    return zipcode;
  }

  public RangeCondition getCapacity() {
    return capacity;
  }

  public RecoveryHousingSearchRequest withCity(final String city) {
    this.city = city;
    return this;
  }

  public RecoveryHousingSearchRequest withZipcode(final String zipcode) {
    this.zipcode = zipcode;
    return this;
  }

  public RecoveryHousingSearchRequest withCapacity(
      final RangeCondition capacity) {
    this.capacity = capacity;
    return this;
  }
}
