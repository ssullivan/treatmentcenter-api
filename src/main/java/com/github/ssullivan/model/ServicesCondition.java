package com.github.ssullivan.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;

public class ServicesCondition {
  private ImmutableSet<String> services;
  private MatchOperator matchOperator;

  public ServicesCondition(final Collection<String> services, final MatchOperator matchOperator) {
    this.services = ImmutableSet.copyOf(services);
    this.matchOperator = matchOperator;
  }

  public ImmutableSet<String> getServices() {
    return services;
  }

  public MatchOperator getMatchOperator() {
    return matchOperator;
  }

  public ServicesCondition union(final ServicesCondition other) {
    if (other == null) {
      return new ServicesCondition(getServices(), this.matchOperator);
    }

    return new ServicesCondition(Sets.union(other.getServices(), this.services),
        this.matchOperator);
  }
}
