package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import java.util.Set;

public class ScoreByMilitaryFamilyStatus implements IScoreFacility {
  private Set<String> serviceCodes;
  private Importance importance;
  private boolean isMilitary;

  public ScoreByMilitaryFamilyStatus(final Set<String> serviceCodes,
      Importance importance, boolean isMilitary) {
    this.serviceCodes = serviceCodes;
    this.importance = importance;
    this.isMilitary = isMilitary;
  }

  public ScoreByMilitaryFamilyStatus(final Set<String> serviceCodes) {
    this(serviceCodes, Importance.NOT, false);
  }

  @Override
  public double score(Facility facility) {
    if (facility == null) return 0.0;
    if (!this.isMilitary || importance == Importance.NOT) {
      return 1.0;
    }
    if (facility.hasAnyOf("MF")) {
      return 1.0;
    }
    if (importance == Importance.SOMEHWAT && !facility.hasAnyOf("MF")) {
      return .8;
    }
    return 0;
  }
}
