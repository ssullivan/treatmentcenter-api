package com.github.ssullivan.core.analytics;

import java.time.LocalDate;
import java.time.Period;
import java.util.function.Function;

public class CalculateAgeMonths implements Function<LocalDate, Integer> {

  /**
   * Returns the age in months from the specified date of birth.
   *
   * @param dateOfBirth the date of birth
   * @return the age in months.
   */
  @Override
  public Integer apply(final LocalDate dateOfBirth) {
    final LocalDate now = LocalDate.now();
    return Period.between(dateOfBirth, now).getMonths();
  }

}
