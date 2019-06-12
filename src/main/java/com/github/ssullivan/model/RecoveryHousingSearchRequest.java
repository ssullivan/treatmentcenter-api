package com.github.ssullivan.model;

import com.github.ssullivan.model.conditions.RangeCondition;
import java.util.Objects;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecoveryHousingSearchRequest that = (RecoveryHousingSearchRequest) o;
    return Objects.equals(getCity(), that.getCity()) &&
        Objects.equals(getZipcode(), that.getZipcode()) &&
        Objects.equals(getCapacity(), that.getCapacity());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCity(), getZipcode(), getCapacity());
  }
}
