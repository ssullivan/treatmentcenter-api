package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import java.time.LocalDate;
import static com.github.ssullivan.core.analytics.Constants.*;

public class ScoreByAge {
  // 25 years - 2 months
  private static final int YOUNG_ADULT_AGE_CEILING_MONTHS = 298;
  // 18 years - 2 months
  private static final int CHILD_AGE_CEILING_MONTHS = 214;

  public double score(final Facility facility, final LocalDate dateOfBirth) {
    final int ageYears = new CalculateAgeYears().apply(dateOfBirth);
    final int ageMonths = new CalculateAgeMonths().apply(dateOfBirth);

    boolean isYoungAdult = isYoungAdult(ageYears, ageMonths);
    boolean isChild = isChild(ageYears, ageMonths);


    if ((ageYears >= 25 && facility.hasService(ADULT)) ||
        (isYoungAdult && facility.hasServices(YOUNG_ADULTS, ADULT)) ||
        (isYoungAdult && facility.hasServices(YOUNG_ADULTS)) ||
        (isChild && facility.hasServices(CHILD, YOUNG_ADULTS)) ||
        (isChild && facility.hasServices(CHILD))) {
      return 1.1;
    }

    return 0.0;
  }

  private boolean isYoungAdult(final int ageYears, final int ageMonths) {
    return ageYears >= 18 && ageMonths <= YOUNG_ADULT_AGE_CEILING_MONTHS;
  }

  private boolean isChild(final int ageYears, final int ageMonths) {
    return ageYears < 18 && ageMonths <= CHILD_AGE_CEILING_MONTHS;
  }
}

