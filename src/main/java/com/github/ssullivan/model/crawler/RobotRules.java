package com.github.ssullivan.model.crawler;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RobotRules {

  private Set<String> disallow;
  private Set<String> allow;

  public RobotRules() {
    this.disallow = new HashSet<>();
    this.allow = new HashSet<>();
  }

  public Set<String> getDisallow() {
    return Collections.unmodifiableSet(disallow);

  }

  public Set<String> getAllow() {
    return Collections.unmodifiableSet(allow);
  }

  public void addDisallow(final String path) {
    this.disallow.add(path);
  }

  public void addAllow(final String path) {
    this.allow.add(path);
  }
}
