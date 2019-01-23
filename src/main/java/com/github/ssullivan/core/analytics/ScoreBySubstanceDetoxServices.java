package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import java.util.Set;

public class ScoreBySubstanceDetoxServices implements IScoreFacility {

  private static final String HOSPITAL_INPATIENT = "HID";
  private static final String RESIDENTIAL = "RD";
  private static final String OUTPATIENT = "OD";

  private static final String BUPRENORPHINE_DETOX = "DB";
  public static final String METHADONE_DETOX = "DM";
  private static final String COCAINE = "CD";
  private static final String METH = "MD";
  private static final String BENZODIAZEPINE = "BD";
  private static final String ALCOHOL = "AD";
  public static final String DETOXIFICATION = "DX";
  public static final String ALCOHOL_DETOX = "ADTX";
  private static final String BENZODIAZEPINES_DETOX = "BDTX";
  public static final String COCAINE_DETOX = "CDTX";
  private static final String METH_DETOX = "MDTX";
  private static final String OPIOIDS_DETOX = "ODTX";

  private final Set<String> serviceCodes;
  private final boolean initialDetox;

  public ScoreBySubstanceDetoxServices(final Set<String> serviceCodes, boolean initialDetox) {
    this.serviceCodes = serviceCodes;
    this.initialDetox = initialDetox;
  }

  public ScoreBySubstanceDetoxServices(Set<String> serviceCodes) {
    this(serviceCodes, false);
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

      if (serviceCodes.contains(COCAINE) && facility.hasService(OPIOIDS_DETOX)) {
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
}
