package com.github.ssullivan.utils;

import java.util.UUID;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class ShortUuidTest {
  @Test
  public void testDecode() {
    final UUID original = UUID.randomUUID();
    final String shortId = ShortUuid.encode(original);
    MatcherAssert.assertThat(shortId, Matchers.notNullValue());

    final UUID decoded = ShortUuid.decode(shortId);
    MatcherAssert.assertThat(decoded, Matchers.equalToObject(original));

  }
}
