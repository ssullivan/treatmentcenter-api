package com.github.ssullivan.core.analytics;

import static com.github.ssullivan.core.analytics.Constants.FEMALE;
import static com.github.ssullivan.core.analytics.Constants.MALE;

import com.github.ssullivan.model.Facility;
import java.util.Set;

public class ScoreByGender implements IScoreFacility {
  private final String gender;

  public ScoreByGender(final String gender) {
    this.gender = gender;
  }

  public ScoreByGender(final Set<String> serviceCodes) {
    this.gender= serviceCodes.stream()
        .filter(it -> FEMALE.equalsIgnoreCase(it) || MALE.equalsIgnoreCase(it))
        .findFirst()
        .orElse("");
  }

  @Override
  public double score(Facility facility) {
    if (facility == null) return 0.0;
    if (gender == null || gender.isEmpty()) {
      return 0;
    }
    switch (gender) {
      case "Female":
      case "female":
      case "FEMALE":
      case FEMALE:
        if (facility.hasAllOf(FEMALE)) {
          return 1.0;
        }
        break;
      case "male":
      case "Male":
      case MALE:
        if (facility.hasAllOf(MALE)) {
          return 1.0;
        }
        break;
      default:
        return 0.0;
    }
    return 0.0;
  }
}
