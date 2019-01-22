package com.github.ssullivan.core.analytics;

import java.util.Set;
import java.util.stream.Stream;

public final class Sets {
  public static boolean anyMatch(final Set<String> set, final String... items) {
    if (null == set || set.isEmpty() || items == null || items.length <= 0) return false;
    return set.stream().anyMatch(item -> Stream.of(items).anyMatch(it -> it.equalsIgnoreCase(item)));
  }

  public static boolean allMatch(final Set<String> set, final String... items) {
    if (null == set || set.isEmpty() || items == null || items.length <= 0) return false;
    return set.stream().allMatch(item -> Stream.of(items).anyMatch(it -> it.equalsIgnoreCase(item)));
  }
}
