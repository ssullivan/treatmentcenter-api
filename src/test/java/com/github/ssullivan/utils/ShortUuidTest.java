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

  @Test
  public void testDecodeThatHasShortByteArray() {
    final String id = "QMbv4BKDzHZ1qn5fKETZmG";
    UUID uuid = ShortUuid.decode(id);
    MatcherAssert.assertThat(uuid.toString(), Matchers.equalTo("e09422bd-8f8d-8702-638e-0f635e91ca27"));

    MatcherAssert.assertThat(ShortUuid.encode(uuid), Matchers.equalTo(id));


    int i = 0;

  }
}
