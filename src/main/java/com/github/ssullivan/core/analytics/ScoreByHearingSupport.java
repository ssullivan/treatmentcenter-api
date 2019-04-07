package com.github.ssullivan.core.analytics;

import com.github.ssullivan.db.postgres.IServiceCodeLookupCache;
import com.github.ssullivan.model.Facility;
import java.util.Set;
import org.jooq.Field;

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
    this.importance = isDeafOrHardOfHearing ? Importance.SOMEWHAT : Importance.NOT;
  }


  public ScoreByHearingSupport(final Set<String> serviceCodes, final Importance importance) {
    this.serviceCodes = serviceCodes;
    this.isDeafOrHardOfHearing = serviceCodes.stream()
        .anyMatch(AH::equalsIgnoreCase);
    this.importance = importance;
  }

  @Override
  public Field<Double> toField(IServiceCodeLookupCache cache)  {
    return PostgresArrayDSL.score(cache, weight(), AH);
  }

  public double weight() {
    if (!isDeafOrHardOfHearing || importance == Importance.NOT) {
      return 1.0;
    }
    if (importance == Importance.SOMEWHAT) {
      return .8;
    }

    return 0;
  }



  @Override
  public double score(Facility facility) {
    if (facility == null) {
      return 0.0;
    }
    if (!isDeafOrHardOfHearing || importance == Importance.NOT || facility.hasAnyOf(AH)) {
      return 1.0;
    }
    if (importance == Importance.SOMEWHAT) {
      return .8;
    }
    return 0;
  }
}
