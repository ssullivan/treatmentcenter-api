package com.github.ssullivan.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ZipGeoQuery {

  @JsonCreator
  public static ZipGeoQuery create(@JsonProperty("zipcode") final String zipcode,
      @JsonProperty("range") final Integer range) {
    return new AutoValue_ZipGeoQuery(zipcode, range);
  }

  /**
   * The zip code to geolocate to calculate a search radius.
   *
   * @return the zipcode
   */
  @JsonProperty("zipcode")
  public abstract String zipcode();

  /**
   * How many miles from the provide zipcode to search for facilities.
   *
   * @return the number of miles
   */
  @JsonProperty("range")
  public abstract Integer range();
}
