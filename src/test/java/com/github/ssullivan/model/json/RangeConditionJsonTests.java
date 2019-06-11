package com.github.ssullivan.model.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.model.conditions.RangeCondition;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import org.eclipse.jetty.util.IO;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class RangeConditionJsonTests {
  private static final ObjectMapper Jackson = new ObjectMapper().findAndRegisterModules();

  @Test
  public void testUndefined() throws IOException {
    RangeCondition fromJson = Jackson.readValue("{}", RangeCondition.class);
    MatcherAssert.assertThat(fromJson, Matchers.nullValue());
  }

  @Test
  public void testIntStartAndStop() throws IOException {
    String rangeJson = createRange(10, 20);
    RangeCondition expected = new RangeCondition(10, 20);
    RangeCondition fromJson = Jackson.readValue(rangeJson, RangeCondition.class);
    MatcherAssert.assertThat(fromJson, Matchers.equalTo(expected));
  }

  @Test
  public void testEquals()  throws IOException {
    RangeCondition expected = new RangeCondition(10, 10);
    RangeCondition fromJson = Jackson.readValue("10", RangeCondition.class);
    MatcherAssert.assertThat(fromJson, Matchers.equalTo(expected));
  }

  @Test
  public void testMixedTypes() throws IOException {
    try {
      Jackson.readValue("{\"start\": 10, \"stop\":\"foo\"}", RangeCondition.class);
    } catch (IOException e) {
      MatcherAssert.assertThat(e, Matchers.instanceOf(IOException.class));
    }
  }

  @Test
  public void testStopBeforeStart() throws Exception {
    try {
      RangeCondition rangeCondition = Jackson.readValue(createRange(0, -1), RangeCondition.class);
    } catch (IllegalArgumentException e) {
      MatcherAssert.assertThat(e, Matchers.instanceOf(IllegalArgumentException.class));
    }
  }

  private String createRange(int start, int stop) throws JsonProcessingException {
    return Jackson.writeValueAsString(ImmutableMap.of("start", start, "stop", stop));
  }
}
