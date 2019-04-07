package com.github.ssullivan.core.analytics;

import com.github.ssullivan.db.postgres.IServiceCodeLookupCache;
import com.github.ssullivan.model.Facility;
import java.util.Set;
import javafx.geometry.Pos;
import org.jooq.Field;
import org.jooq.impl.DSL;

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

  @Override
  public Field<Double> toField(IServiceCodeLookupCache cache) {
    if (!this.isMilitary || importance == Importance.NOT) {
      return DSL.one().cast(Double.class);
    }

    // not sure how to handle the ! yet
    return PostgresArrayDSL.score(cache, 1.0, "MF");
  }
}
