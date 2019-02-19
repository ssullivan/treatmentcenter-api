package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import java.util.Set;
import java.util.stream.Collectors;

public class ScoreByLang implements IScoreFacility {

  private static final String FX = "FX";
  private static final String NX = "NX";
  private static final String SP = "SP";
  private static final String N13 = "N13";
  private static final String N18 = "N18";
  private static final String N23 = "N23";
  private static final String N24 = "N24";
  private static final String N40 = "N40";
  private static final String F4 = "F4";
  private static final String F17 = "F17";
  private static final String F19 = "F19";
  private static final String F25 = "F25";
  private static final String F28 = "F28";
  private static final String F30 = "F30";
  private static final String F31 = "F31";
  private static final String F35 = "F35";
  private static final String F36 = "F36";
  private static final String F37 = "F37";
  private static final String F42 = "F42";
  private static final String F43 = "F43";
  private static final String F47 = "F47";
  private static final String F66 = "F66";
  private static final String F67 = "F67";
  private static final String F70 = "F70";
  private static final String F81 = "F81";
  private static final String F92 = "F92";

  private static final String[] LANGS = new String[]{
      N13,
      N18,
      N23,
      N24,
      N40,
      F4,
      F17,
      F19,
      F25,
      F28,
      F30,
      F31,
      F35,
      F36,
      F37,
      F42,
      F43,
      F47,
      F66,
      F67,
      F70,
      F81,
      F92
  };
  private final Set<String> selectedLangs;
  private boolean isEnglishFirst;
  private Importance importance;

  public ScoreByLang(final Set<String> serviceCodes, final boolean isEnglishFirst,
      Importance importance) {
    this.isEnglishFirst = isEnglishFirst;
    this.importance = importance;
    this.selectedLangs = langCodes(serviceCodes);
  }

  public ScoreByLang(final Set<String> serviceCodes) {
    this.isEnglishFirst = serviceCodes
        .stream()
        .noneMatch(ScoreByLang::isLang);

    if (!isEnglishFirst) {
      this.importance = Importance.SOMEWHAT;
    }
    this.selectedLangs = langCodes(serviceCodes);
  }

  public ScoreByLang(final Set<String> serviceCodes, final Importance importance) {
    this.isEnglishFirst = serviceCodes
        .stream()
        .noneMatch(ScoreByLang::isLang);

    this.importance = importance;
    this.selectedLangs = langCodes(serviceCodes);
  }

  private static boolean isLang(final String serviceCode) {
    if (serviceCode == null || serviceCode.isEmpty()) {
      return false;
    }
    switch (serviceCode) {
      case N13:
        return true;
      case N18:
        return true;
      case N23:
        return true;
      case N24:
        return true;
      case N40:
        return true;
      case F4:
        return true;
      case F17:
        return true;
      case F19:
        return true;
      case F25:
        return true;
      case F28:
        return true;
      case F30:
        return true;
      case F31:
        return true;
      case F35:
        return true;
      case F36:
        return true;
      case F37:
        return true;
      case F42:
        return true;
      case F43:
        return true;
      case F47:
        return true;
      case F66:
        return true;
      case F67:
        return true;
      case F70:
        return true;
      case F81:
        return true;
      case F92:
        return true;
      default:
        return false;
    }
  }

  private static Set<String> langCodes(final Set<String> serviceCodes) {
    return serviceCodes.stream().filter(ScoreByLang::isLang).collect(Collectors.toSet());
  }

  @Override
  public double score(Facility facility) {
    if (facility == null) {
      return 0.0;
    }
    if (this.isEnglishFirst || (importance == Importance.NOT)) {
      return 1.0;
    }

    if (!selectedLangs.isEmpty() && facility.hasAnyOf(selectedLangs)) {
      return 1.0;
    }

    if (!this.isEnglishFirst
        && importance == Importance.SOMEWHAT
        && !selectedLangs.isEmpty()
        && !facility.hasAnyOf(selectedLangs)) {
      return .8;
    }

    return 0;
  }
}
