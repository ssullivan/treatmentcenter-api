package com.github.ssullivan.core.analytics;

import com.github.ssullivan.db.postgres.IServiceCodeLookupCache;
import com.github.ssullivan.model.Facility;
import java.util.Set;
import org.jooq.Field;
import org.jooq.impl.DSL;

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

  @Override
  public Field<Double> toField(IServiceCodeLookupCache cache) {
    if (serviceCodes.contains("IRL")) {
      return PostgresArrayDSL.score(cache, 1.0, "RL");
    }
    if (serviceCodes.contains("IRS")) {
      return PostgresArrayDSL.score(cache, 1.0, "RS");
    }
    if (serviceCodes.contains("IOIT")) {
      return PostgresArrayDSL.score(cache, 1.0, "OIT");
    }
    if (serviceCodes.contains("IORT")) {
      return PostgresArrayDSL.score(cache, 1.0, "IORT");
    }
    return DSL.zero().cast(Double.class);
  }
}
