package com.github.ssullivan.model.crawler;

import java.util.HashMap;
import java.util.Map;

public class RobotsTxt {
  private final Map<String, RobotRules> rules = new HashMap<>();

  public RobotsTxt() {
  }

  public void addRules(final String userAgent, final RobotRules robotRules) {
    this.rules.put(userAgent, robotRules);
  }

  public RobotRules getRules(final String userAgent) {
    return this.rules.get(userAgent);
  }
}
