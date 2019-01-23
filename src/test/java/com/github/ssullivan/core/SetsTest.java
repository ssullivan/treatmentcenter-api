package com.github.ssullivan.core;

import com.github.ssullivan.core.analytics.Sets;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class SetsTest {
  @Test
  public void testAnyOfInt() {
    MatcherAssert.assertThat(Sets.anyMatch(ImmutableSet.of(1, 2, 3), 1, 2),
        Matchers.equalTo(true));
  }

  @Test
  public void testAnyOfString() {
    MatcherAssert.assertThat(Sets.anyMatch(ImmutableSet.of("1", "2", "3"), "1", "2"),
        Matchers.equalTo(true));
  }

  @Test
  public void testAllOfInt() {
    MatcherAssert.assertThat(Sets.allMatch(ImmutableSet.of(1, 2, 3), 1, 2),
        Matchers.equalTo(true));
  }

  @Test
  public void testAllOfString() {
    MatcherAssert.assertThat(Sets.allMatch(ImmutableSet.of("1", "2", "3"), "1", "2"),
        Matchers.equalTo(true));
  }
}
