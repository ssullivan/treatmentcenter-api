package com.github.ssullivan.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import java.util.Collection;
import java.util.Set;

public class ServicesCondition {

  private ImmutableSet<String> serviceCodes;
  private ImmutableSet<String> mustNotServiceCodes;
  private MatchOperator matchOperator;

  /**
   * If any of the service codes in the set start with a ! those will be negated from the search results.
   *
   * These are valid
   * !a,b,c,!d
   *
   * or
   * a,b,c,d,e
   *
   * @param serviceCodes a list of service codes
   */
  public ServicesCondition(final Collection<String> serviceCodes, final MatchOperator matchOperator) {
    final ImmutableSet.Builder<String> serviceCodesBuilder = new Builder<>();
    final ImmutableSet.Builder<String> mustNotServiceCodes = new Builder<>();

    for (final String serviceCode : serviceCodes) {
      if (serviceCode.startsWith("!")) {
        mustNotServiceCodes.add(serviceCode.substring(1).trim());
      }
      else {
        serviceCodesBuilder.add(serviceCode.trim());
      }
    }
    this.serviceCodes = serviceCodesBuilder.build();
    this.mustNotServiceCodes = mustNotServiceCodes.build();
    this.matchOperator = matchOperator;
  }

  public Set<String> getServices() {
    return ImmutableSet.copyOf(this.serviceCodes);
  }

  public ImmutableSet<String> getServiceCodes() {
    return serviceCodes;
  }

  public ImmutableSet<String> getMustNotServiceCodes() {
    return mustNotServiceCodes;
  }

  public MatchOperator getMatchOperator() {
    return matchOperator;
  }

  public int size() {
    return serviceCodes.size() + mustNotServiceCodes.size();
  }
}
