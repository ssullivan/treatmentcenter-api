package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import java.util.Set;

public class ScoreByMilitaryStatus implements IScoreFacility {

  private boolean isMilitary;
  private boolean noMilitaryService;
  private Importance importance;
  private Set<String> serviceCodes;

  public ScoreByMilitaryStatus(final Set<String> serviceCodes, final Importance importance) {
    this.isMilitary = Sets.anyMatch(serviceCodes, "AD", "GR", "IVET");
    this.noMilitaryService = Sets.anyMatch(serviceCodes, "NMS");
    this.importance = importance;
    this.serviceCodes = serviceCodes;
  }

  @Override
  public double score(Facility facility) {
    if (this.noMilitaryService
        || (Sets.anyMatch(serviceCodes, "AD", "GR", "IVET") && importance == Importance.NOT)
        || (Sets.anyMatch(serviceCodes, "AD") && facility.hasAnyOf("ADM"))
        || (Sets.anyMatch(serviceCodes, "IVET", "GR") && facility.hasAnyOf("VET"))) {
      return 1.0;
    }

    if ((Sets.allMatch(serviceCodes, "AD", "MSI") && !facility.hasAnyOf("ADM"))
        || (Sets.anyMatch(serviceCodes, "IVET", "GR") && !facility.hasAnyOf("VET"))) {
      return 0.8;
    }

    // the provided docs for scoring had other conditions but they all result in 0
    // so for performance just return 0

    return 0;
  }
}
