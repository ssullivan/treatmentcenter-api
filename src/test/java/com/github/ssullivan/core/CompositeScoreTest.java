package com.github.ssullivan.core;

import com.github.ssullivan.core.analytics.CompositeFacilityScore;
import com.github.ssullivan.model.Facility;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class CompositeScoreTest {

  @Test
  public void testScoring() {
    final Set<String> codes = ImmutableSet.of("METH");
    final Facility facility = new Facility();
    facility.setServiceCodes(ImmutableSet.of("METH", "MALE", "FEMLE", "FOC"));
    final CompositeFacilityScore compositeFacilityScore = new CompositeFacilityScore(codes);
    double score = compositeFacilityScore.score(facility);
    MatcherAssert.assertThat(score, Matchers.greaterThanOrEqualTo(4.0));
  }
}
