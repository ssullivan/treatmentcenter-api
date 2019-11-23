package com.github.ssullivan.core.analytics;

import com.github.ssullivan.db.postgres.IServiceCodeLookupCache;
import com.google.common.base.Joiner;
import java.util.Collection;
import org.jooq.Field;
import org.jooq.impl.DSL;

public final class PostgresArrayDSL {

  public static Field<Double> score(final IServiceCodeLookupCache cache, final double weight,
      final String... serviceCodes) {
    if (serviceCodes == null || serviceCodes.length == 0) {
      return DSL.zero().cast(Double.class);
    }

    return DSL.field("(?::int[] && services)::int * ?", Boolean.class,
        "{" + Joiner.on(",").join(cache.lookupSet(serviceCodes)) + "}", weight)
        .cast(Double.class);
  }

  public static Field<Double> scoreIfItContains(final IServiceCodeLookupCache cache,
      final double weightMatches, final double weightMissing, final String... serviceCodes) {
    return DSL.field("CASE services @> ?::int[] WHEN true THEN ? ELSE ? END", Double.class,
        '{' + Joiner.on(",").join(cache.lookupSet(serviceCodes)) + '}',
        weightMatches,
        weightMissing);
  }

  public static Field<Double> score(final IServiceCodeLookupCache cache, final double weight,
      final Collection<String> serviceCodes) {
    if (serviceCodes == null || serviceCodes.isEmpty()) {
      return DSL.zero().cast(Double.class);
    }
    return DSL.field("(?::int[] && services)::int * ?", Boolean.class,
        "{" + Joiner.on(",").join(cache.lookupSet(serviceCodes)) + "}", weight)
        .cast(Double.class);
  }
}
