package com.github.ssullivan.model;

import com.github.ssullivan.model.conditions.RangeCondition;
import java.util.Objects;

public class RecoveryHousingSearchRequest {
  private String state;
  private String city;
  private String zipcode;
  private String gender;
  private RangeCondition capacity;

  public RecoveryHousingSearchRequest() {
  }

  public String getState() {
    return state;
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

  public String getGender() {
    return gender;
  }

  public RecoveryHousingSearchRequest withState(final String state) {
    this.state = state;
    return this;
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

  public RecoveryHousingSearchRequest withGender(final String gender) {
    this.gender = gender;
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
