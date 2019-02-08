package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import java.util.Set;

public class ScoreByMentalHealth implements IScoreFacility {

  private final Set<String> serviceCodes;
  private final boolean mentalHealthRelated;


  public ScoreByMentalHealth(Set<String> serviceCodes) {
    this.serviceCodes = serviceCodes;
    this.mentalHealthRelated = Sets.anyMatch(serviceCodes, "GHF", "MHF", "MHSAF", "SAF",
        "CBT", "DBT", "REBT", "SACA", "TRC", "MHS", "CSAA", "SMHD");
  }

  @Override
  public double score(Facility facility) {
    if (facility == null) {
      return 0.0;
    }
    if (mentalHealthRelated && facility.hasAnyOf("MHSAF", "MHF", "CO")) {
      return 1.0;
    }
    return 0.0;
  }
}
