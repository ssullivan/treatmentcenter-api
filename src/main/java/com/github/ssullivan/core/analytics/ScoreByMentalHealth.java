package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import java.util.Set;

public class ScoreByMentalHealth implements IScoreFacility {
  private final Set<String> serviceCodes;
  private final boolean mentalHealthRelated;

  public ScoreByMentalHealth(final Set<String> serviceCodes, final boolean mentalHealthRelated) {
    this.serviceCodes = serviceCodes;
    this.mentalHealthRelated = mentalHealthRelated;
  }

  public ScoreByMentalHealth(Set<String> serviceCodes) {
    this(serviceCodes, Sets.anyMatch(serviceCodes, "GHF", "MHF", "MHSAF", "SAF",
        "CBT", "DBT", "REBT", "SACA", "TRC", "MHS", "CSAA", "SMHD"));
  }

  @Override
  public double score(Facility facility) {
    if (mentalHealthRelated && facility.hasAnyOf("MHSAF", "MHF", "CO")) {
      return 1.0;
    }
    return 0.0;
  }
}
