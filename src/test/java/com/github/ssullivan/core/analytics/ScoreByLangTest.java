package com.github.ssullivan.core.analytics;

import com.github.ssullivan.model.Facility;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class ScoreByLangTest {
  @Test
  public void testScoreByLang() {
    ScoreByLang scoreByLang = new ScoreByLang(ImmutableSet.of("F17"), false, Importance.VERY);
    Facility facility = new Facility();
    facility.setServiceCodes(ImmutableSet.of("F17", "F23"));

    double score = scoreByLang.score(facility);
    MatcherAssert.assertThat(score, Matchers.greaterThanOrEqualTo(1.0));
  }
}
