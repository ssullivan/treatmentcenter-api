package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import java.util.Set;

public class ScoreByServiceSetting implements IScoreFacility {

  private final Set<String> serviceCodes;

  public ScoreByServiceSetting(final Set<String> serviceCodes) {
    this.serviceCodes = serviceCodes;
  }

  @Override
  public double score(Facility facility) {
    if (facility == null) {
      return 0.0;
    }
    if (serviceCodes.contains("IRL") && facility.hasService("RL")
        || serviceCodes.contains("IRS") && facility.hasService("RS")
        || serviceCodes.contains("IOIT") && facility.hasService("OIT")
        || serviceCodes.contains("IORT") && facility.hasService("IORT")) {
      return 1.0;
    }

    return 0;
  }
}
