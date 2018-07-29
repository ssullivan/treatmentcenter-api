package com.github.ssullivan.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.annotation.Nullable;

@AutoValue
public abstract class FacilitySearchQuery {

  @JsonCreator
  public static FacilitySearchQuery create(
      @JsonProperty("serviceCodes") final Set<String> serviceCodes,
      @JsonProperty("zipGeoQuery") final ZipGeoQuery zipGeoQuery) {
    return new AutoValue_FacilitySearchQuery(ImmutableSet.copyOf(serviceCodes), zipGeoQuery);
  }

  /**
   * A list of services that facilities must have.
   *
   * @return service codes
   */
  @Nullable
  public abstract ImmutableSet<String> serviceCodes();


  /**
   * A geographic range to bound the facility search by.
   *
   * @return the zip geo query
   */
  @Nullable
  public abstract ZipGeoQuery zipGeoQuery();
}
