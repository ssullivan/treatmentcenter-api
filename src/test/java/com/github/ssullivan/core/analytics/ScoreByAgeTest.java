package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import com.google.common.collect.ImmutableSet;
import java.time.LocalDate;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class ScoreByAgeTest {
  private final LocalDate AdultDateOfBirth = LocalDate.now().minusYears(30);
  private final LocalDate YouthDateOfBirth = LocalDate.now().minusYears(18);
  private final LocalDate ChildDateOfBirth = LocalDate.now().minusYears(10);

  @Test
  public void testAgeAdult() {

    ScoreByAge scoreByAge = new ScoreByAge(AdultDateOfBirth);
    Facility facility = new Facility();
    facility.setServiceCodes(ImmutableSet.of("ADLT"));

    double score = scoreByAge.score(facility);
    MatcherAssert.assertThat(score, Matchers.greaterThanOrEqualTo(1.0));
  }

  @Test
  public void testAgeAdultNoCode() {
    ScoreByAge scoreByAge = new ScoreByAge(AdultDateOfBirth);
    Facility facility = new Facility();
    facility.setServiceCodes(ImmutableSet.of("ADLT"));

    double score = scoreByAge.score(facility);
    MatcherAssert.assertThat(score, Matchers.greaterThanOrEqualTo(1.0));
  }

  @Test
  public void testAgeYouth() {

  }

  public void testAgeChild() {

  }
}
