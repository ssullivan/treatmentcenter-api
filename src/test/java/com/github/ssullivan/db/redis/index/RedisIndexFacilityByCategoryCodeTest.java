package com.github.ssullivan.db.redis.index;

import com.github.ssullivan.db.IndexFacilityByCategoryCode;
import com.github.ssullivan.db.IndexFacilityByServiceCode;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.GeoPoint;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class RedisIndexFacilityByCategoryCodeTest extends AbstractRedisIndexTest {
  @Test
  public void indexLocationByServiceCodeInvalidFacilityId() throws IOException {
    try {
      final IndexFacilityByCategoryCode index = injector.getInstance(IndexFacilityByCategoryCode.class);
      Facility facility = new Facility();
      facility.setFeedId(FeedId);
      facility.setId("0");
      facility.setLocation(GeoPoint.geoPoint(64.0, 64.0));
      facility.setServiceCodes(ImmutableSet.of("TEST"));
      facility.setCategoryCodes(ImmutableSet.of("TEST"));

      index.index(FeedId, facility);
    } catch (IOException e) {
      MatcherAssert.assertThat(e.getCause(), Matchers.instanceOf(IllegalArgumentException.class));
    }
  }
}
