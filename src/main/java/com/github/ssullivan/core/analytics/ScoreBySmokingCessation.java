package com.github.ssullivan.core.analytics;

import com.github.ssullivan.db.postgres.IServiceCodeLookupCache;
import com.github.ssullivan.model.Facility;
import com.google.common.base.Joiner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javafx.geometry.Pos;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScoreBySmokingCessation implements IScoreFacility {
  private static final Logger LOGGER = LoggerFactory.getLogger(ScoreBySmokingCessation.class);

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

  @Override
  public Field<Double> toField(IServiceCodeLookupCache cache) {
    try {
      if (importance == Importance.SOMEWHAT) {
        return DSL.field("CASE services @> ?::int[] WHEN false THEN .8 ELSE 0 END", Double.class,
            '{' + Joiner.on(",").join(cache.lookupSet("NRT", "NSC", "STU", "TCC")) + '}')
            .cast(Double.class);
      }

      return PostgresArrayDSL.score(cache, 1.0, "NRT", "NSC", "STU", "TCC");
    }
    catch (RuntimeException e) {
      LOGGER.error("", e);
    }
    return DSL.zero().cast(Double.class);
  }
}
