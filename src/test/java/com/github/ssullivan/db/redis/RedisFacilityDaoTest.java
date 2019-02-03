package com.github.ssullivan.db.redis;

import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.db.redis.RedisCategoryCodesDao;
import com.github.ssullivan.db.redis.RedisFacilityDao;
import com.github.ssullivan.db.redis.RedisFeedDao;
import com.github.ssullivan.db.redis.RedisServiceCodeDao;
import com.github.ssullivan.guice.RedisClientModule;
import com.github.ssullivan.model.Category;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.FacilityWithRadius;
import com.github.ssullivan.model.GeoPoint;
import com.github.ssullivan.model.MatchOperator;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchRequest;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.Service;
import com.github.ssullivan.model.ServicesCondition;
import com.github.ssullivan.utils.ShortUuid;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class RedisFacilityDaoTest {
  private static final String FirstId = ShortUuid.randomShortUuid();
  private static final String SecondId = ShortUuid.randomShortUuid();
  private static final String ThirdId = ShortUuid.randomShortUuid();

  private RedisFacilityDao _dao;
  private RedisCategoryCodesDao _categoryCodesDao;
  private RedisServiceCodeDao _serviceCodesDao;
  private IRedisConnectionPool _redisConnectionPool;

  @BeforeAll
  private void setup() {
    final Injector injector = Guice
        .createInjector(new RedisClientModule(new RedisConfig("127.0.0.1", 6379, 1)));
    _dao = injector.getInstance(RedisFacilityDao.class);
    _categoryCodesDao = injector.getInstance(RedisCategoryCodesDao.class);
    _serviceCodesDao = injector.getInstance(RedisServiceCodeDao.class);
    _redisConnectionPool = injector.getInstance(IRedisConnectionPool.class);

  }


  @AfterEach
  private void flushDb() throws Exception {
    _redisConnectionPool.borrowConnection().sync().flushdb();
  }

  @AfterAll
  private void teardown() throws Exception {
    _redisConnectionPool.close();
  }


  @Test
  public void testAddingFacility() throws IOException {
    final Facility original = new Facility();
    original.setId(FirstId);
    original.setCategoryCodes(Sets.newHashSet("TEST"));
    original.setServiceCodes(Sets.newHashSet("BAR"));
    original.setCity("New York");
    original.setState("NY");
    original.setFormattedAddress("Test St. 1234");
    original.setWebsite("http://www.test.com");
    original.setZip("10001");

    _dao.addFacility(original);

    final Facility fromDb = _dao.getFacility(original.getId());
    MatcherAssert.assertThat(fromDb, Matchers.notNullValue());
    MatcherAssert.assertThat(fromDb.getId(), Matchers.equalTo(original.getId()));
    MatcherAssert.assertThat(fromDb.getCategoryCodes(), Matchers.containsInAnyOrder("TEST"));
    MatcherAssert.assertThat(fromDb.getServiceCodes(), Matchers.containsInAnyOrder("BAR"));
    MatcherAssert.assertThat(fromDb.getCity(), Matchers.equalToIgnoringCase(original.getCity()));
    MatcherAssert.assertThat(fromDb.getState(), Matchers.equalTo(original.getState()));
    MatcherAssert
        .assertThat(fromDb.getFormattedAddress(), Matchers.equalTo(original.getFormattedAddress()));
    MatcherAssert.assertThat(fromDb.getWebsite(), Matchers.equalTo(original.getWebsite()));
    MatcherAssert.assertThat(fromDb.getZip(), Matchers.equalTo(original.getZip()));

  }
}
