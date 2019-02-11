package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import java.util.Set;

public class ScoreBySmokingPolicy implements IScoreFacility {

  private static final String SMON = "SMON";
  private static final String SMPD = "SMPD";
  private static final String SMOP = "SMOP";

  private Set<String> serviceCodes;
  private boolean isSmoker;

  public ScoreBySmokingPolicy(Set<String> serviceCodes, boolean isSmoker) {
    this.serviceCodes = serviceCodes;
    this.isSmoker = isSmoker;
  }

  public ScoreBySmokingPolicy(Set<String> serviceCodes) {
    this(serviceCodes, Sets.anyMatch(serviceCodes, SMON, SMPD, SMOP));
  }

  @Override
  public double score(Facility facility) {
    if (facility == null) {
      return 0.0;
    }
    if (isSmoker) {
      if (Sets.anyMatch(serviceCodes, SMON) && facility.hasAnyOf(SMON, SMPD, SMOP)) {
        return 1.0;
      }
      if (Sets.anyMatch(serviceCodes, SMPD) && facility.hasAnyOf(SMPD, SMOP)) {
        return 1.0;
      }
      if (Sets.anyMatch(serviceCodes, SMOP) && facility.hasAnyOf(SMOP)) {
        return 1.0;
      }
    } else {
      if (Sets.anyMatch(serviceCodes, SMON) && facility.hasAnyOf(SMON)) {
        return 1.0;
      }
      if (Sets.anyMatch(serviceCodes, SMPD) && facility.hasAnyOf(SMON, SMPD)) {
        return 1.0;
      }
      if (Sets.anyMatch(serviceCodes, SMOP) && facility.hasAnyOf(SMOP, SMON, SMPD)) {
        return 1.0;
      }
    }
    return 0;
  }
}
