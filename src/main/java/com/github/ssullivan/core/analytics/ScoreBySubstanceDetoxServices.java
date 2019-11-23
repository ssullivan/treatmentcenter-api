package com.github.ssullivan.core.analytics;

import com.github.ssullivan.db.postgres.IServiceCodeLookupCache;
import com.github.ssullivan.model.Facility;
import java.util.Set;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class ScoreBySubstanceDetoxServices implements IScoreFacility {

  public static final String METHADONE_DETOX = "DM";
  public static final String DETOXIFICATION = "DX";
  public static final String ALCOHOL_DETOX = "ADTX";
  public static final String COCAINE_DETOX = "CDTX";
  private static final String HOSPITAL_INPATIENT = "HID";
  private static final String RESIDENTIAL = "RD";
  private static final String OUTPATIENT = "OD";
  private static final String BUPRENORPHINE_DETOX = "DB";
  private static final String COCAINE = "CD";
  private static final String METH = "MD";
  private static final String BENZODIAZEPINE = "BD";
  private static final String ALCOHOL = "AD";
  private static final String BENZODIAZEPINES_DETOX = "BDTX";
  private static final String METH_DETOX = "MDTX";
  private static final String OPIOIDS_DETOX = "ODTX";

  private final Set<String> serviceCodes;
  private final boolean initialDetox;

  public ScoreBySubstanceDetoxServices(final Set<String> serviceCodes, final boolean initialDetox) {
    this.serviceCodes = serviceCodes;
    this.initialDetox = initialDetox;
  }

  public ScoreBySubstanceDetoxServices(final Set<String> serviceCodes) {
    this(serviceCodes,
        Sets.anyMatch(serviceCodes, BUPRENORPHINE_DETOX, COCAINE_DETOX, METH_DETOX, OPIOIDS_DETOX));
  }

  @Override
  public double score(final Facility facility) {
    if (!initialDetox) {
      if (facility.hasAnyOf(BUPRENORPHINE_DETOX, HOSPITAL_INPATIENT, RESIDENTIAL)) {
        return 1.0;
      }

      if (serviceCodes.contains(OUTPATIENT) && facility.hasAllOf(OPIOIDS_DETOX)) {
        return 1.0;
      }

      if (serviceCodes.contains(COCAINE) && facility.hasService(COCAINE_DETOX)) {
        return 1.0;
      }

      if (serviceCodes.contains(METH) && facility.hasService(METH_DETOX)) {
        return 1.0;
      }

      if (serviceCodes.contains(BENZODIAZEPINE) && facility.hasService(BENZODIAZEPINES_DETOX)) {
        return 1.0;
      }

      if (serviceCodes.contains(ALCOHOL) && facility.hasService(ALCOHOL_DETOX)) {
        return 1.0;
      }
    }
    return 0;
  }

  @Override
  public Field<Double> toField(IServiceCodeLookupCache cache) {
    if (!initialDetox) {

      if (serviceCodes.contains(OUTPATIENT)) {
        return PostgresArrayDSL.score(cache, 1.0, OPIOIDS_DETOX);
      }

      if (serviceCodes.contains(COCAINE)) {
        return PostgresArrayDSL.score(cache, 1.0, COCAINE_DETOX);
      }

      if (serviceCodes.contains(METH)) {
        return PostgresArrayDSL.score(cache, 1.0, METH_DETOX);
      }

      if (serviceCodes.contains(BENZODIAZEPINE)) {
        return PostgresArrayDSL.score(cache, 1.0, BENZODIAZEPINES_DETOX);
      }

      if (serviceCodes.contains(ALCOHOL)) {
        return PostgresArrayDSL.score(cache, 1.0, ALCOHOL_DETOX);
      }

      return PostgresArrayDSL
          .score(cache, 1.0, BUPRENORPHINE_DETOX, HOSPITAL_INPATIENT, RESIDENTIAL);
    }
    return DSL.zero().cast(Double.class);
  }
}
