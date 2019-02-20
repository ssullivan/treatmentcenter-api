package com.github.ssullivan.db.redis.index;

import com.github.ssullivan.db.IndexFacility;
import com.github.ssullivan.db.IndexFacilityByGeo;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.GeoPoint;
import java.io.IOException;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class RedisFacilityIndexDaoTest extends AbstractRedisIndexTest {

  @Test
  public void testIndexWithInvalidId() throws IOException {
    try {
      final IndexFacility indexFacilityByGeo = injector.getInstance(IndexFacility.class);
      Facility facility = new Facility();
      facility.setFeedId(FeedId);
      facility.setId("0");
      facility.setLocation(GeoPoint.geoPoint(64.0, 64.0));

      indexFacilityByGeo.index(FeedId, facility);
    } catch (IOException e) {
      MatcherAssert.assertThat(e.getCause(), Matchers.instanceOf(IllegalArgumentException.class));
    }
  }

}
