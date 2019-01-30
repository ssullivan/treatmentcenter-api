package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.FacilityWithRadius;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class ScoreByMilStatusTest {
  @Test
  public void testScoringByMilStatus() {
    final ScoreByMilitaryStatus s = new ScoreByMilitaryStatus(ImmutableSet.of("IVET", "AD"), Importance.VERY);
    Facility facility = new FacilityWithRadius();
    facility.setServiceCodes(ImmutableSet.of("FOO"));

    double score = s.score(facility);

    MatcherAssert.assertThat(score, Matchers.lessThanOrEqualTo(0.8));
  }
}
