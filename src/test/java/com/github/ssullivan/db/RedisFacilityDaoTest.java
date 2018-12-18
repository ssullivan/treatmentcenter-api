package com.github.ssullivan.db;

import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.db.redis.RedisFacilityDao;
import com.github.ssullivan.guice.RedisClientModule;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.FacilityWithRadius;
import com.github.ssullivan.model.GeoPoint;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class RedisFacilityDaoTest {

  private RedisFacilityDao _dao;
  private IRedisConnectionPool _redisConnectionPool;

  @BeforeAll
  private void setup() {
    final Injector injector = Guice
        .createInjector(new RedisClientModule(new RedisConfig("127.0.0.1", 6379)));
    _dao = injector.getInstance(RedisFacilityDao.class);
    _redisConnectionPool = injector.getInstance(IRedisConnectionPool.class);
  }

  @AfterAll
  private void teardown() throws Exception {
    _redisConnectionPool.close();
  }

  @Test
  public void testAddingFacility() throws IOException {
    final Facility original = new Facility();
    original.setId(1);
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

  @Test
  public void testSearchingForFacilityByServiceCode() throws IOException {
    final Facility original = new Facility();
    original.setId(99);
    original.setCategoryCodes(Sets.newHashSet("TEST"));
    original.setServiceCodes(Sets.newHashSet("BIZZ"));
    original.setCity("New York");
    original.setState("NY");
    original.setFormattedAddress("Test St. 1234");
    original.setWebsite("http://www.test.com");
    original.setZip("10001");

    _dao.addFacility(original);
    final SearchResults ret = _dao.findByServiceCodes(ImmutableSet.of("BIZZ"), Page.page());
    MatcherAssert.assertThat(ret, Matchers.notNullValue());
    MatcherAssert.assertThat(ret.totalHits(), Matchers.equalTo(1L));

    final Facility fromDb = (Facility) ret.hits().get(0);
    MatcherAssert.assertThat(fromDb, Matchers.notNullValue());
    MatcherAssert.assertThat(fromDb.getId(), Matchers.equalTo(original.getId()));
    MatcherAssert.assertThat(fromDb.getCategoryCodes(), Matchers.containsInAnyOrder("TEST"));
    MatcherAssert.assertThat(fromDb.getServiceCodes(), Matchers.containsInAnyOrder("BIZZ"));
    MatcherAssert.assertThat(fromDb.getCity(), Matchers.equalToIgnoringCase(original.getCity()));
    MatcherAssert.assertThat(fromDb.getState(), Matchers.equalTo(original.getState()));
    MatcherAssert
        .assertThat(fromDb.getFormattedAddress(), Matchers.equalTo(original.getFormattedAddress()));
    MatcherAssert.assertThat(fromDb.getWebsite(), Matchers.equalTo(original.getWebsite()));
    MatcherAssert.assertThat(fromDb.getZip(), Matchers.equalTo(original.getZip()));
  }

  @Test
  public void testSearchingForFacilityByGeo() throws IOException {
    final Facility original = new Facility();
    original.setId(1);
    original.setCategoryCodes(Sets.newHashSet("TEST"));
    original.setServiceCodes(Sets.newHashSet("BAR"));
    original.setCity("New York");
    original.setState("NY");
    original.setFormattedAddress("Test St. 1234");
    original.setWebsite("http://www.test.com");
    original.setZip("10001");
    original.setLocation(GeoPoint.geoPoint(40.715076, -73.991180));

    _dao.addFacility(original);

    SearchResults<FacilityWithRadius> ret =
        _dao.findByServiceCodesWithin(ImmutableList.of("BAR"), -73.991, 40.715, 30, "km",
            Page.page());

    final Facility fromDb = (Facility) ret.hits().get(0);
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

  @Test
  public void testSearchingForFacilityByGeoNegate() throws IOException {
    final Facility original1 = new Facility();
    original1.setId(1);
    original1.setCategoryCodes(Sets.newHashSet("TEST"));
    original1.setServiceCodes(Sets.newHashSet("BAR"));
    original1.setCity("New York");
    original1.setState("NY");
    original1.setFormattedAddress("Test St. 1234");
    original1.setWebsite("http://www.test.com");
    original1.setZip("10001");
    original1.setLocation(GeoPoint.geoPoint(40.715076, -73.991180));

    final Facility original2 = new Facility();
    original2.setId(2);
    original2.setCategoryCodes(Sets.newHashSet("TEST"));
    original2.setServiceCodes(Sets.newHashSet("BAR", "BUZZ"));
    original2.setCity("New York");
    original2.setState("NY");
    original2.setFormattedAddress("Bar St. 1234");
    original2.setWebsite("http://www.test.com");
    original2.setZip("10001");
    original2.setLocation(GeoPoint.geoPoint(40.715076, -73.991180));

    final Facility original3 = new Facility();
    original3.setId(3);
    original3.setCategoryCodes(Sets.newHashSet("TEST"));
    original3.setServiceCodes(Sets.newHashSet("BAR", "FIZZ"));
    original3.setCity("New York");
    original3.setState("NY");
    original3.setFormattedAddress("Fizz St. 1234");
    original3.setWebsite("http://www.test.com");
    original3.setZip("10001");
    original3.setLocation(GeoPoint.geoPoint(40.715076, -73.991180));

    _dao.addFacility(original1);
    _dao.addFacility(original2);
    _dao.addFacility(original3);

    SearchResults<FacilityWithRadius> ret =
        _dao.findByServiceCodesWithin(ImmutableList.of("BAR"), ImmutableList.of("FIZZ"),
            -73.991, 40.715, 31, "km",
            Page.page());


    MatcherAssert.assertThat(ret.hits(), Matchers.hasSize(2));
    final Facility fromDb = (Facility) ret.hits().get(0);
    MatcherAssert.assertThat(fromDb, Matchers.notNullValue());
    MatcherAssert.assertThat(fromDb.getId(), Matchers.equalTo(original1.getId()));
    MatcherAssert.assertThat(fromDb.getCategoryCodes(), Matchers.containsInAnyOrder("TEST"));
    MatcherAssert.assertThat(fromDb.getServiceCodes(), Matchers.containsInAnyOrder("BAR"));
    MatcherAssert.assertThat(fromDb.getCity(), Matchers.equalToIgnoringCase(original1.getCity()));
    MatcherAssert.assertThat(fromDb.getState(), Matchers.equalTo(original1.getState()));
    MatcherAssert
        .assertThat(fromDb.getFormattedAddress(), Matchers.equalTo(original1.getFormattedAddress()));
    MatcherAssert.assertThat(fromDb.getWebsite(), Matchers.equalTo(original1.getWebsite()));
    MatcherAssert.assertThat(fromDb.getZip(), Matchers.equalTo(original1.getZip()));
  }
}
