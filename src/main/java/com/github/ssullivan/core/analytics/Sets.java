package com.github.ssullivan.core.analytics;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

public final class Sets {

  public static <T> boolean anyMatch(final Set<T> set, final T... items) {
    if (null == set || set.isEmpty() || items == null || items.length <= 0) {
      return false;
    }
    final Set<T> distinct = ImmutableSet.copyOf(items);
    return set.stream().anyMatch(distinct::contains);
  }

  public static <T> boolean allMatch(final Set<T> set, final T... items) {
    if (null == set || set.isEmpty() || items == null || items.length <= 0) {
      return false;
    }
    final Set<T> distinct = ImmutableSet.copyOf(items);
    return distinct.size() == distinct.stream().map(set::contains).filter(it -> it).count();
  }
}
