package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class SmokingCessationScoreTest {

  @Test
  public void testSmokingSupportSomewhat() {
    ScoreBySmokingCessation scoreBySmokingCessation = new ScoreBySmokingCessation(
        ImmutableSet.of("NRT"), Importance.SOMEWHAT);

    Facility facility = new Facility();
    facility.setServiceCodes(ImmutableSet.of("FOO"));
    double score = scoreBySmokingCessation.score(facility);

    MatcherAssert.assertThat(score, Matchers.equalTo(0.8));
    int j = 0;
  }
}
