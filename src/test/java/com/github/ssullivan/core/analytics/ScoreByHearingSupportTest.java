package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class ScoreByHearingSupportTest {

  @Test
  public void testScore() {
    ScoreByHearingSupport scoreByHearingSupport = new ScoreByHearingSupport(new HashSet<>(), true, Importance.VERY);
    Facility facility = new Facility();
    facility.setServiceCodes(ImmutableSet.of("AH"));

    double score = scoreByHearingSupport.score(facility);

    MatcherAssert.assertThat(score, Matchers.greaterThanOrEqualTo(1.0));
  }

  @Test
  public void testScoreNoCode() {
    ScoreByHearingSupport scoreByHearingSupport = new ScoreByHearingSupport(new HashSet<>(), true, Importance.VERY);
    Facility facility = new Facility();
    facility.setServiceCodes(ImmutableSet.of(""));

    double score = scoreByHearingSupport.score(facility);

    MatcherAssert.assertThat(score, Matchers.lessThanOrEqualTo(0.0));
  }

}
