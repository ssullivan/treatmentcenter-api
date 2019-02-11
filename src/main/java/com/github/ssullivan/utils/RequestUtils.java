package com.github.ssullivan.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class RequestUtils {

  /**
   * This function will take a List of strings and return a new List of strings. If any of the
   * individual strings contain comma separators than this function will break those out into their
   * own strings in the new list
   *
   * For Example if we had a List like
   *
   * "a,b","c","d"
   *
   * the resulting List would be "a","b","c","d"
   */
  public static List<String> flatten(List<String> items) {
    if (items == null) {
      return ImmutableList.of();
    }

    ImmutableList.Builder<String> builder = new Builder<>();
    items.stream()
        .flatMap((Function<String, Stream<String>>) s -> {
          if (s.contains(",")) {
            ImmutableList.Builder<String> sublist = new ImmutableList.Builder<>();
            sublist.add(s.split(","));
            return sublist.build().stream();
          }
          return ImmutableList.of(s).stream();
        })
        .map(String::trim)
        .forEach(builder::add);
    return builder.build();
  }
}
