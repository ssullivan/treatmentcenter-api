package com.github.ssullivan.db.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IServiceCodesDao;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.utils.ShortUuid;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.lettuce.core.KeyValue;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.IOException;
import org.assertj.core.util.Lists;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mockito;

@TestInstance(Lifecycle.PER_CLASS)
public class RedisFacilityDaoTest {

  private static final String FeedId = ShortUuid.randomShortUuid();
  private static final String FirstId = ShortUuid.randomShortUuid();

  private static final com.fasterxml.jackson.databind.ObjectMapper ObjectMapper = new ObjectMapper();
  private IRedisConnectionPool redisPool;
  private StatefulRedisConnection<String, String> redisConnection;
  private RedisCommands<String, String> redisCommand;
  private Injector injector;
  private ICategoryCodesDao categoryCodesDao;
  private IFeedDao feedDao;
  private IServiceCodesDao serviceCodesDao;
  private IFacilityDao dao;


  @BeforeEach()
  public void initEach() throws Exception {
    RedisConfig redisConfig = new RedisConfig();
    redisConfig.setTimeout(5L);

    redisPool = Mockito.mock(IRedisConnectionPool.class);
    redisConnection = (StatefulRedisConnection<String, String>) Mockito
        .mock(StatefulRedisConnection.class);
    redisCommand = (RedisCommands<String, String>) Mockito.mock(RedisCommands.class);

    Mockito.when(redisConnection.sync()).thenReturn(redisCommand);
    Mockito.when(redisPool.borrowConnection()).thenReturn(redisConnection);
    Mockito.when(redisPool.borrowConnection(Mockito.anyLong())).thenReturn(redisConnection);

    categoryCodesDao = Mockito.mock(ICategoryCodesDao.class);
    serviceCodesDao = Mockito.mock(IServiceCodesDao.class);
    feedDao = Mockito.mock(IFeedDao.class);

    injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(RedisConfig.class).toInstance(redisConfig);
        bind(IRedisConnectionPool.class).toInstance(redisPool);
        bind(ObjectMapper.class).toInstance(ObjectMapper);

        bind(ICategoryCodesDao.class).toInstance(categoryCodesDao);
        bind(IServiceCodesDao.class).toInstance(serviceCodesDao);
        bind(IFeedDao.class).toInstance(feedDao);
      }
    });

    dao = injector.getInstance(IFacilityDao.class);
  }


  @Test
  public void testAddingFacility() throws IOException {
    final Facility original = new Facility();
    original.setId(FirstId);
    original.setFeedId(FeedId);
    original.setCategoryCodes(Sets.newHashSet("TEST"));
    original.setServiceCodes(Sets.newHashSet("BAR"));
    original.setCity("New York");
    original.setState("NY");
    original.setFormattedAddress("Test St. 1234");
    original.setWebsite("http://www.test.com");
    original.setZip("10001");

    final String json = ObjectMapper.writeValueAsString(original);

    Mockito.when(redisCommand.hmget(Mockito.anyString(), Mockito.eq("_source")))
        .thenReturn(Lists.newArrayList(KeyValue.just("_source", json)));

    dao.addFacility(FeedId, original);

    final Facility fromDb = dao.getFacility(original.getId());
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
