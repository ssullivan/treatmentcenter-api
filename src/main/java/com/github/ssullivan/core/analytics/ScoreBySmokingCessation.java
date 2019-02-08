package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import java.util.Set;

public class ScoreBySmokingCessation implements IScoreFacility {

  private static final String NRT = "NRT";
  private static final String NSC = "NSC";
  private static final String STU = "STU";
  private static final String TCC = "TCC";
  private static final String STCC = "STCC";
  private static final String VTCC = "VTCC";

  private final Set<String> serviceCodes;
  private final boolean smokingCessation;
  private Importance importance;

  public ScoreBySmokingCessation(final Set<String> serviceCodes, final boolean smokingCessation) {
    this.serviceCodes = serviceCodes;
    this.smokingCessation = smokingCessation;
    if (this.smokingCessation) {
      this.importance = Importance.SOMEWHAT;
    } else {
      this.importance = Importance.NOT;
    }
  }


  public ScoreBySmokingCessation(final Set<String> serviceCodes, final boolean smokingCessation,
      final Importance importance) {
    this.serviceCodes = serviceCodes;
    this.smokingCessation = smokingCessation;
    this.importance = importance;
  }

  public ScoreBySmokingCessation(Set<String> serviceCodes) {
    this(serviceCodes, Sets.anyMatch(serviceCodes, NRT, NSC, STCC, VTCC));
  }

  public ScoreBySmokingCessation(Set<String> serviceCodes, Importance importance) {
    this(serviceCodes, Sets.anyMatch(serviceCodes, NRT, NSC, STCC, VTCC), importance);
  }

  @Override
  public double score(final Facility facility) {
    if (facility == null) {
      return 0.0;
    }
    if (this.smokingCessation) {
      if (facility.hasAnyOf(NRT, NSC, STU, TCC)) {
        return 1.0;
      }
      if (importance == Importance.SOMEWHAT && !facility.hasAnyOf(NRT, NSC, STU, TCC)) {
        return .8;
      }
      // ???
//      if (Sets.anyMatch(serviceCodes, VTCC) && !facility.hasAnyOf(NRT, NSC, STU, TCC)) {
//
//      }
    }
    return 0;
  }
}
