package com.github.ssullivan.core.analytics;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

/**
 * Utility methods for checking if sets contain elements.
 */
public final class Sets {

  /**
   * Returns true if set contains any of the items.
   *
   * @param set the set to check
   * @param items the items to check for in the set
   * @param <T> the type of the elements
   * @return true if the set contains any of the elements from items
   */
  public static <T> boolean anyMatch(final Set<T> set, final T... items) {
    if (null == set || set.isEmpty() || items == null || items.length <= 0) {
      return false;
    }
    final Set<T> distinct = ImmutableSet.copyOf(items);
    return set.stream().anyMatch(distinct::contains);
  }

  /**
   * Returns true if the set contains all of the items.
   *
   * @param set the set to check
   * @param items the items to check for in the set
   * @param <T> the type of the elements
   * @return true if the set contains all of the elements from items
   */
  public static <T> boolean allMatch(final Set<T> set, final T... items) {
    if (null == set || set.isEmpty() || items == null || items.length <= 0) {
      return false;
    }
    final Set<T> distinct = ImmutableSet.copyOf(items);
    return distinct.size() == distinct.stream().map(set::contains).filter(it -> it).count();
  }
}
