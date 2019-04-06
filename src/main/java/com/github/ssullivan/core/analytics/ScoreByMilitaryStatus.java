package com.github.ssullivan.core.analytics;

import com.github.ssullivan.db.postgres.IServiceCodeLookupCache;
import com.github.ssullivan.model.Facility;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.util.postgres.PostgresDSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScoreByMilitaryStatus implements IScoreFacility {
  private static final Logger LOGGER = LoggerFactory.getLogger(ScoreByMilitaryStatus.class);

  private boolean noMilitaryService;
  private Importance importance;
  private Set<String> serviceCodes;

  public ScoreByMilitaryStatus(final Set<String> serviceCodes, final Importance importance) {
    this.noMilitaryService = Sets.anyMatch(serviceCodes, "NMS");
    this.importance = importance;
    this.serviceCodes = serviceCodes;
  }

  @Override
  public double score(Facility facility) {
    if (this.noMilitaryService
        || (Sets.anyMatch(serviceCodes, "AD", "GR", "VET") && importance == Importance.NOT)
        || (Sets.anyMatch(serviceCodes, "AD") && facility.hasAnyOf("ADM"))
        || (Sets.anyMatch(serviceCodes, "VET", "GR") && facility.hasAnyOf("VET"))) {
      return 1.0;
    }

    if ((Sets.allMatch(serviceCodes, "AD", "MSI") && !facility.hasAnyOf("ADM"))
        || (Sets.anyMatch(serviceCodes, "VET", "GR") && !facility.hasAnyOf("VET"))) {
      return 0.8;
    }

    // the provided docs for scoring had other conditions but they all result in 0
    // so for performance just return 0

    return 0;
  }

  @Override
  public Field<Double> toField(IServiceCodeLookupCache cache) {
    if (this.noMilitaryService
        || (Sets.anyMatch(serviceCodes, "AD", "GR", "VET") && importance == Importance.NOT)
        || (Sets.anyMatch(serviceCodes, "AD"))
        || (Sets.anyMatch(serviceCodes, "VET", "GR"))) {
      return PostgresArrayDSL.score(cache, 1.0, "ADM", "VET");
    }

    try {
      if ((Sets.allMatch(serviceCodes, "AD", "MSI"))) {
        return DSL.field("CASE services @> ?::int[] WHEN true THEN .8 ELSE 0 END", Double.class,
            '{' + cache.lookup("ADM") + '}');
      }

      if (Sets.anyMatch(serviceCodes, "VET", "GR")) {
        return DSL.field("CASE services @> ?::int[] WHEN true THEN .8 ELSE 0 END", Double.class,
            '{' + cache.lookup("VET") + '}');
      }
    }
    catch (ExecutionException e) {
      LOGGER.error("Failed to find service code", e);
    }

    return DSL.zero().cast(Double.class);
  }
}
