package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import java.util.Set;

public class ScoreByHearingSupport implements IScoreFacility {
  private static final String AH = "AH";
  private final Set<String> serviceCodes;
  private final boolean isDeafOrHardOfHearing;
  private final Importance importance;

  public ScoreByHearingSupport(final Set<String> serviceCodes,
      final boolean isDeafOrHardOfHearing, Importance importance) {
    this.serviceCodes = serviceCodes;
    this.isDeafOrHardOfHearing = isDeafOrHardOfHearing;
    this.importance = importance;
  }

  public ScoreByHearingSupport(Set<String> serviceCodes) {
    this(serviceCodes, false, Importance.NOT);
  }

  @Override
  public double score(Facility facility) {
    if (!isDeafOrHardOfHearing || importance == Importance.NOT || facility.hasAnyOf(AH)) {
      return 1.0;
    }
    if (importance == Importance.SOMEHWAT) {
      return .8;
    }
    return 0;
  }
}
