package com.github.ssullivan.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;

public class ServicesConditionFactory {

  public ImmutableList<ServicesCondition> fromRequestParams(final List<String> serviceCodes, final List<String> matchAny) {
    final ImmutableList.Builder<ServicesCondition> builder = new ImmutableList.Builder<>();
    builder.addAll(fromList(serviceCodes, MatchOperator.MUST));
    builder.addAll(fromList(matchAny, MatchOperator.SHOULD));
    return builder.build();
  }

  private List<ServicesCondition> fromList(final List<String> serviceCodes, final MatchOperator matchOperator) {
    if (null == serviceCodes) {
      return ImmutableList.of();
    }
    final ImmutableList.Builder<ServicesCondition> builder = new Builder<>();
    serviceCodes.stream()
        .map(s -> {
              if (s.contains(",")) {
                return new ServicesCondition(ImmutableList.copyOf(s.split(",")), matchOperator);
              } else {
                return new ServicesCondition(ImmutableList.of(s), matchOperator);
              }
            }
        )
        .forEach(builder::add);
    return builder.build();
  }
}
