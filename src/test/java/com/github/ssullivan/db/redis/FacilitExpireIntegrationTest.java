package com.github.ssullivan.db.redis;

import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IndexFacility;
import com.github.ssullivan.guice.RedisClientModule;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.utils.ShortUuid;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.lettuce.core.RedisClient;
import java.io.IOException;
import java.util.List;
import org.assertj.core.util.Lists;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class FacilitExpireIntegrationTest {
  private static final String FeedId = "tvkT3WN7nBbnpPHredaHb2";

  @Test
  public void testBatching() throws IOException {
    final RedisConfig redisConfig = new RedisConfig("localhost", 6379, 6);
    redisConfig.setTimeout(250);

    final Injector injector = Guice
        .createInjector(new RedisClientModule(redisConfig));

    final RedisClient client = injector.getInstance(RedisClient.class);
    client.connect().sync().flushdb();

    final Facility facility1 = new Facility();
    facility1.setFeedId(FeedId);
    facility1.setId("ExjcnoDU9W5hWSsVVYxh2Y");
    facility1.setName1("TEST");
    facility1.setName2("TEST");
    facility1.setCategoryCodes(ImmutableSet.of("TEST"));
    facility1.setServiceCodes(ImmutableSet.of("TEST"));

    final Facility facility2 = new Facility();
    facility2.setFeedId(FeedId);
    facility2.setId("Xy5wA2uC687wuL4L2d9A8Q");
    facility2.setName1("TEST");
    facility2.setName2("TEST");
    facility2.setCategoryCodes(ImmutableSet.of("TEST"));
    facility2.setServiceCodes(ImmutableSet.of("TEST"));

    IFacilityDao facilityDao = injector.getInstance(IFacilityDao.class);
    facilityDao.addFacility(FeedId, Lists.newArrayList(facility1, facility2));

    final List<Facility> batch =
        facilityDao.fetchBatch(Lists.newArrayList(facility1.getId(), facility2.getId()));

    MatcherAssert.assertThat(batch, Matchers.notNullValue());
    MatcherAssert.assertThat(batch, Matchers.hasSize(2));

    final Facility first = batch.get(0);

    final Facility second = batch.get(1);

    MatcherAssert.assertThat(first.getId(), Matchers.equalTo(facility1.getId()));
    MatcherAssert.assertThat(second.getId(), Matchers.equalTo(facility2.getId()));
  }

  @Test
  public void testExpireKeys() throws Exception {
    final RedisConfig redisConfig = new RedisConfig("localhost", 6379, 5);
    redisConfig.setTimeout(250);


    final Injector injector = Guice
        .createInjector(new RedisClientModule(redisConfig));

    final RedisClient client = injector.getInstance(RedisClient.class);
    client.connect().sync().flushdb();



    final IFacilityDao facilityDao = injector.getInstance(IFacilityDao.class);

    final Facility facility = new Facility();
    facility.setFeedId(FeedId);
    facility.setId(ShortUuid.randomShortUuid());
    facility.setName1("TEST");
    facility.setName2("TEST");
    facility.setCategoryCodes(ImmutableSet.of("TEST"));
    facility.setServiceCodes(ImmutableSet.of("TEST"));

    facilityDao.addFacility(FeedId, facility);

    final Facility fromRedis = facilityDao.getFacility(facility.getId());
    MatcherAssert.assertThat(fromRedis.getId(), Matchers.equalTo(facility.getId()));
    MatcherAssert.assertThat(fromRedis.getName1(), Matchers.equalTo(facility.getName1()));
    MatcherAssert.assertThat(fromRedis.getName2(), Matchers.equalTo(facility.getName2()));
    final Boolean result = facilityDao.expire(FeedId, 1, false);

    final IndexFacility indexFacility = injector.getInstance(IndexFacility.class);
    indexFacility.expire(FeedId, 1, false);

    MatcherAssert.assertThat(result, Matchers.equalTo(true));

    int retries = 0;
    do {
      List<String> keys = client.connect().sync().keys("*");
      if (keys == null || keys.size() <= 0) {
        break;
      }
      Thread.sleep(150 + (retries * 5));
    } while (++retries < 10);


    List<String> keys = client.connect().sync().keys("*");
    MatcherAssert.assertThat(keys.size(), Matchers.lessThanOrEqualTo(0));

    final Facility fromRedisAfterExpire = facilityDao.getFacility(facility.getId());
    MatcherAssert.assertThat(fromRedisAfterExpire, Matchers.nullValue());
  }
}
