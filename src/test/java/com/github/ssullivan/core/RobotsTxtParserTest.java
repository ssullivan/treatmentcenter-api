package com.github.ssullivan.core;

import com.github.ssullivan.model.crawler.RobotRules;
import com.github.ssullivan.model.crawler.RobotsTxt;
import io.dropwizard.testing.FixtureHelpers;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class RobotsTxtParserTest {
  @Test
  public void testParsing() throws IOException {
    final String fixture = FixtureHelpers.fixture("fixtures/Robots.txt");
    final RobotsTxtParser parser = new RobotsTxtParser();
    final RobotsTxt robotsTxt = parser.parse(new ByteArrayInputStream(fixture.getBytes()));

    final RobotRules robotRules = robotsTxt.getRules("Some_Annoying_Bot");
    MatcherAssert.assertThat(robotRules, Matchers.notNullValue());
    MatcherAssert.assertThat(robotRules.getDisallow(), Matchers.containsInAnyOrder("/"));
  }
}
