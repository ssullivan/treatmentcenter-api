package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class ScoreByMedsTest {
  @Test
  public void testScore() {
    ScoreByMedAssistedTreatment s = new ScoreByMedAssistedTreatment(ImmutableSet.of("IMETH", "METH"));
    Facility facility = new Facility();
    facility.setServiceCodes(ImmutableSet.of("METH"));

    double score = s.score(facility);
    MatcherAssert.assertThat(score, Matchers.greaterThanOrEqualTo(1.0));
  }

}
