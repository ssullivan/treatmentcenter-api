package com.github.ssullivan.db;

import com.github.ssullivan.utils.RequestUtils;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class RequestUtilsTest {

  @Test
  public void testFlattenWithItemThatHasCommas() {
    final List<String> original = ImmutableList.of("a,b", "c");
    final List<String> transformed = RequestUtils.flatten(original);

    MatcherAssert.assertThat(transformed,

        Matchers.contains("a", "b", "c")
    );
  }

  @Test
  public void testFlattenWithItemThatDoesntHaveCommas() {
    final List<String> original = ImmutableList.of("a", "b", "c");
    final List<String> transformed = RequestUtils.flatten(original);

    MatcherAssert.assertThat(transformed,

        Matchers.contains("a", "b", "c")
    );
  }

  @Test
  public void testFlattenNullList() {
    final List<String> transformed = RequestUtils.flatten(null);
    MatcherAssert.assertThat(transformed, Matchers.empty());
  }
}
