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

  public ScoreByHearingSupport(final Set<String> serviceCodes) {
    this.serviceCodes = serviceCodes;
    this.isDeafOrHardOfHearing = serviceCodes.stream()
        .anyMatch(AH::equalsIgnoreCase);
    this.importance = isDeafOrHardOfHearing ? Importance.SOMEHWAT : Importance.NOT;
  }


  public ScoreByHearingSupport(final Set<String> serviceCodes, final Importance importance) {
    this.serviceCodes = serviceCodes;
    this.isDeafOrHardOfHearing = serviceCodes.stream()
        .anyMatch(AH::equalsIgnoreCase);
    this.importance = importance;
  }


  @Override
  public double score(Facility facility) {
    if (facility == null) return 0.0;
    if (!isDeafOrHardOfHearing || importance == Importance.NOT || facility.hasAnyOf(AH)) {
      return 1.0;
    }
    if (importance == Importance.SOMEHWAT) {
      return .8;
    }
    return 0;
  }
}
