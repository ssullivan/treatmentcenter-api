package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;

public class ScoreByTraumaServices implements IScoreFacility {
  private boolean needsSupport;
  private Set<TraumaTypes> traumaTypes;
  private Set<String> serviceCodes;

  public ScoreByTraumaServices(boolean needsSupport,
      Set<TraumaTypes> traumaTypes, Set<String> serviceCodes) {
    this.needsSupport = needsSupport;
    this.traumaTypes = traumaTypes;
    this.serviceCodes = serviceCodes;
  }

  public ScoreByTraumaServices(boolean needsSupport,
      List<TraumaTypes> traumaTypes, Set<String> serviceCodes) {
    this.needsSupport = needsSupport;
    this.traumaTypes = ImmutableSet.copyOf(traumaTypes);
    this.serviceCodes = serviceCodes;
  }

  public ScoreByTraumaServices(final Set<String> serviceCodes) {
    this(false, ImmutableSet.of(), serviceCodes);
  }

  @Override
  public double score(final Facility facility) {
    if (facility == null) return 0.0;
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
}
