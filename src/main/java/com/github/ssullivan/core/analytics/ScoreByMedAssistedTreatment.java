package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import java.util.Set;

public class ScoreByMedAssistedTreatment implements IScoreFacility {
  private static final String BMW = "BMW";
  private static final String BU = "BU";
  private static final String BUM = "BUM";
  private static final String BUP = "BUP";
  private static final String METH = "METH";
  private static final String MM = "MM";
  private static final String MMW = "MMW";
  private static final String NMOA = "NMOA";
  private static final String NXN = "NXN";
  private static final String PAIN = "PAIN";
  private static final String RPN = "RPN";
  private static final String UBN = "UBN";
  private static final String VTRL = "VTRL";
  private static final String IMETH = "IMETH";
  private static final String NALT = "NALT";
  private static final String VIV = "VIV";

  private static final String[] MED_CODES = new String[] {
      BMW, BU, BUP, BUM, METH, MM, MMW, NXN, PAIN, RPN, UBN, VTRL, NALT, IMETH, VIV, NMOA
  };


  private final Set<String> serviceCodes;
  private final boolean useMeds;

  public ScoreByMedAssistedTreatment(final Set<String> serviceCodes) {
    this.serviceCodes = serviceCodes;
    this.useMeds = Sets.anyMatch(serviceCodes, MED_CODES);
  }

  @Override
  public double score(final Facility facility) {
    if (facility == null) return 0.0;
    if (this.useMeds) {
      if (noPref() && usesMeds(facility)) {
        return 1.0;
      }
      if (Sets.anyMatch(serviceCodes, BUP) && facility.hasAnyOf(BMW, BU, BUM, PAIN, UBN)) {
        return 1.0;
      }
      if (Sets.anyMatch(serviceCodes, IMETH) && facility.hasAnyOf(METH, MM, MMW, PAIN)) {
        return 1.0;
      }
      if (Sets.anyMatch(serviceCodes, NALT) && facility.hasAnyOf(NXN, RPN, UBN)) {
        return 1.0;
      }
      if (Sets.anyMatch(serviceCodes, VIV) && facility.hasAnyOf(VTRL)) {
        return 1.0;
      }
    }
    return 0;
  }

  private boolean usesMeds(final Facility facility) {
    return facility.hasAnyOf(MED_CODES) && !facility.hasAnyOf(NMOA);
  }

  private boolean noPref() {
    return serviceCodes == null || serviceCodes.isEmpty();
  }


}
