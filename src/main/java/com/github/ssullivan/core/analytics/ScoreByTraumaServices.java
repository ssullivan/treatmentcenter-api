package com.github.ssullivan.core.analytics;

import com.github.ssullivan.db.postgres.IServiceCodeLookupCache;
import com.github.ssullivan.model.Facility;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class ScoreByTraumaServices implements IScoreFacility {

  private boolean needsSupport;
  private Set<TraumaTypes> traumaTypes;

  public ScoreByTraumaServices(boolean needsSupport,
      Set<TraumaTypes> traumaTypes, Set<String> serviceCodes) {
    this.needsSupport = needsSupport;
    this.traumaTypes = traumaTypes;
  }

  public ScoreByTraumaServices(boolean needsSupport,
      List<TraumaTypes> traumaTypes, Set<String> serviceCodes) {
    this.needsSupport = needsSupport;
    this.traumaTypes = ImmutableSet.copyOf(traumaTypes);
  }

  public ScoreByTraumaServices(final Set<String> serviceCodes) {
    this(false, ImmutableSet.of(), serviceCodes);
  }

  @Override
  public double score(final Facility facility) {
    if (facility == null) {
      return 0.0;
    }
    if (!needsSupport) {
      return 1.0;
    }

    if (this.traumaTypes.contains(TraumaTypes.TRAUMA) && facility.hasAnyOf("TRMA", "TRC")) {
      return 1.0;
    }
    if (this.traumaTypes.contains(TraumaTypes.DOMESTIC) && facility.hasAnyOf("DV", "DVFP")) {
      return 1.0;
    }
    if (this.traumaTypes.contains(TraumaTypes.SEXUAL) && facility.hasAnyOf("IXA")) {
      return 1.0;
    }
    boolean hasTraumaSupport = facility.hasAnyOf("TRMA", "TRC");
    if (Sets.anyMatch(traumaTypes, TraumaTypes.TRAUMA, TraumaTypes.DOMESTIC)
        && hasTraumaSupport
        && facility.hasAnyOf("DV", "DVFP")) {
      return 1.0;
    }
    if (Sets.anyMatch(traumaTypes, TraumaTypes.TRAUMA, TraumaTypes.SEXUAL)
        && hasTraumaSupport
        && facility.hasAnyOf("XA")
    ) {
      return 1.0;
    }
    if (Sets.anyMatch(traumaTypes, TraumaTypes.DOMESTIC, TraumaTypes.SEXUAL, TraumaTypes.TRAUMA)
        && hasTraumaSupport
        && facility.hasAnyOf("DV", "DVP")
        && facility.hasAnyOf("XA")) {
      return 1.0;
    }
    return 0;
  }

  @Override
  public Field<Double> toField(IServiceCodeLookupCache cache) {
    if (!needsSupport) {
      return DSL.one().cast(Double.class);
    }

    if (this.traumaTypes.contains(TraumaTypes.TRAUMA)) {
      return PostgresArrayDSL.score(cache, 1.0, "TRMA", "TRC");
    }
    if (this.traumaTypes.contains(TraumaTypes.DOMESTIC)) {
      return PostgresArrayDSL.score(cache, 1.0, "DV", "DVFP");
    }
    if (this.traumaTypes.contains(TraumaTypes.SEXUAL)) {
      return PostgresArrayDSL.score(cache, 1.0, "IXA");
    }

    if (Sets.anyMatch(traumaTypes, TraumaTypes.TRAUMA, TraumaTypes.DOMESTIC)) {
      return PostgresArrayDSL.score(cache, 1.0, "DV", "DVFP");
    }
    if (Sets.anyMatch(traumaTypes, TraumaTypes.TRAUMA, TraumaTypes.SEXUAL)) {
      return PostgresArrayDSL.score(cache, 1.0, "XA");
    }
    if (Sets.anyMatch(traumaTypes, TraumaTypes.DOMESTIC, TraumaTypes.SEXUAL, TraumaTypes.TRAUMA)) {
      return PostgresArrayDSL.score(cache, 1.0, "DV", "DVP", "XA", "TRMA", "TRC");
    }

    return DSL.zero().cast(Double.class);
  }
}
