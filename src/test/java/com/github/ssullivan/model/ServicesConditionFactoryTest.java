package com.github.ssullivan.model;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

class ServicesConditionFactoryTest {

  @Test
  public void testMultipleServiceCodeSets() {
    final List<String> input = ImmutableList.of("a,b,d,e", "x", "y", "z");
    ServicesConditionFactory factory = new ServicesConditionFactory();
    ImmutableList<ServicesCondition> conditions =
        factory.fromRequestParams(input, ImmutableList.of());

    MatcherAssert.assertThat(conditions, Matchers.hasSize(4));
    MatcherAssert.assertThat(conditions.get(0).getServices(),
        Matchers.containsInAnyOrder("a", "b", "d", "e"));
  }

  @Test
  public void testMultipleServiceCodeSetsWithNegation() {
    final List<String> input = ImmutableList.of("a,!b,!d,e", "x", "y", "z");
    ServicesConditionFactory factory = new ServicesConditionFactory();
    ImmutableList<ServicesCondition> conditions =
        factory.fromRequestParams(input, ImmutableList.of());

    MatcherAssert.assertThat(conditions, Matchers.hasSize(4));
    MatcherAssert.assertThat(conditions.get(0).getServices(),
        Matchers.containsInAnyOrder("a", "e"));
    MatcherAssert.assertThat(conditions.get(0).getMustNotServiceCodes(),
        Matchers.containsInAnyOrder("b", "d"));
  }

  @Test
  public void testNullCollection() {
    ServicesConditionFactory factory = new ServicesConditionFactory();
    final ImmutableList<ServicesCondition> conditions = factory.fromRequestParams(null, null);
    MatcherAssert.assertThat(conditions, Matchers.notNullValue());
    MatcherAssert.assertThat(conditions, Matchers.hasSize(0));
  }
}
