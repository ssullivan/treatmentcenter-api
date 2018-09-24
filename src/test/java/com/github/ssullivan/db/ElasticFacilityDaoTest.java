package com.github.ssullivan.db;

import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.db.redis.RedisFacilityDao;
import com.github.ssullivan.guice.RedisClientModule;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class ElasticFacilityDaoTest {
  private RedisFacilityDao _dao;

  @BeforeAll
  private void setup() {
    final Injector injector = Guice.createInjector(new RedisClientModule(new RedisConfig("127.0.0.1", 6379)));
    _dao = injector.getInstance(RedisFacilityDao.class);

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
    MatcherAssert.assertThat(fromDb.getFormattedAddress(), Matchers.equalTo(original.getFormattedAddress()));
    MatcherAssert.assertThat(fromDb.getWebsite(), Matchers.equalTo(original.getWebsite()));
    MatcherAssert.assertThat(fromDb.getZip(), Matchers.equalTo(original.getZip()));

  }

  @Test
  public void testSearchingForFacilityByServiceCode() throws IOException {
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
    final SearchResults ret = _dao.findByServiceCodes(ImmutableSet.of("BAR"), Page.page());
    MatcherAssert.assertThat(ret, Matchers.notNullValue());
    MatcherAssert.assertThat(ret.totalHits(), Matchers.equalTo(1L));

    final Facility fromDb = (Facility) ret.hits().get(0);
    MatcherAssert.assertThat(fromDb, Matchers.notNullValue());
    MatcherAssert.assertThat(fromDb.getId(), Matchers.equalTo(original.getId()));
    MatcherAssert.assertThat(fromDb.getCategoryCodes(), Matchers.containsInAnyOrder("TEST"));
    MatcherAssert.assertThat(fromDb.getServiceCodes(), Matchers.containsInAnyOrder("BAR"));
    MatcherAssert.assertThat(fromDb.getCity(), Matchers.equalToIgnoringCase(original.getCity()));
    MatcherAssert.assertThat(fromDb.getState(), Matchers.equalTo(original.getState()));
    MatcherAssert.assertThat(fromDb.getFormattedAddress(), Matchers.equalTo(original.getFormattedAddress()));
    MatcherAssert.assertThat(fromDb.getWebsite(), Matchers.equalTo(original.getWebsite()));
    MatcherAssert.assertThat(fromDb.getZip(), Matchers.equalTo(original.getZip()));
  }
}
