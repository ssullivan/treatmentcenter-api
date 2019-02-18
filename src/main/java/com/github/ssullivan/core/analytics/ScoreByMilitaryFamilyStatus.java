package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import java.util.Set;

public class ScoreByMilitaryFamilyStatus implements IScoreFacility {
  private Importance importance;
  private boolean isMilitary;


  public ScoreByMilitaryFamilyStatus(final Set<String> serviceCodes) {
    this.isMilitary = serviceCodes.stream().anyMatch("MF"::equalsIgnoreCase);
    this.importance = isMilitary ? Importance.VERY : Importance.NOT;
  }

  public ScoreByMilitaryFamilyStatus(final Set<String> serviceCodes, final Importance importance) {
    this.isMilitary = serviceCodes.stream().anyMatch("MF"::equalsIgnoreCase);
    this.importance = importance;
  }

  @Override
  public double score(Facility facility) {
    if (facility == null) {
      return 0.0;
    }
    if (!this.isMilitary || importance == Importance.NOT) {
      return 1.0;
    }
    if (facility.hasAnyOf("MF")) {
      return 1.0;
    }
    if (importance == Importance.SOMEWHAT && !facility.hasAnyOf("MF")) {
      return .8;
    }
    return 0;
  }
}
