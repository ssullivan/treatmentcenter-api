package com.github.ssullivan.core.analytics;

import com.github.ssullivan.db.postgres.IServiceCodeLookupCache;
import com.github.ssullivan.model.Facility;
import java.util.Set;
import org.jooq.Field;
import org.jooq.impl.DSL;

public interface IScoreFacility {

  double score(final Facility facility);

  default Field<Double> toField(IServiceCodeLookupCache cache) {
    return DSL.zero().cast(Double.class);
  }

}
