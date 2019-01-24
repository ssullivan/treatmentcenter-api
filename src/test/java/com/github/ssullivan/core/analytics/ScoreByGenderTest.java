package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class ScoreByGenderTest {

  @Test
  public void testScoreMale() {
    final Facility facility = new Facility();
    facility.setServiceCodes(ImmutableSet.of("MALE"));

    double score = new ScoreByGender("MALE").score(facility);
    MatcherAssert.assertThat(score, Matchers.greaterThanOrEqualTo(1.0));
  }

  @Test
  public void testScoreFemale() {
    final Facility facility = new Facility();
    facility.setServiceCodes(ImmutableSet.of("FEM"));

    double score = new ScoreByGender("FEMALE").score(facility);
    MatcherAssert.assertThat(score, Matchers.greaterThanOrEqualTo(1.0));
  }

  @Test
  public void testScoreMaleNoCode() {
    final Facility facility = new Facility();
    facility.setServiceCodes(ImmutableSet.of("FEM"));

    double score = new ScoreByGender("MALE").score(facility);
    MatcherAssert.assertThat(score, Matchers.lessThanOrEqualTo(0.0));
  }

  @Test
  public void testScoreFemaleNoCode() {
    final Facility facility = new Facility();
    facility.setServiceCodes(ImmutableSet.of("MALE"));

    double score = new ScoreByGender("FEMALE").score(facility);
    MatcherAssert.assertThat(score, Matchers.lessThanOrEqualTo(0.0));
  }
}
