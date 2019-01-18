package com.github.ssullivan.db;

import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.db.redis.RedisCategoryCodesDao;
import com.github.ssullivan.db.redis.RedisFacilityDao;
import com.github.ssullivan.db.redis.RedisServiceCodeDao;
import com.github.ssullivan.guice.RedisClientModule;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.FacilityWithRadius;
import com.github.ssullivan.model.MatchOperator;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchRequest;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.ServicesCondition;
import com.github.ssullivan.tasks.LoadCategoriesAndServicesFunctor;
import com.github.ssullivan.tasks.LoadTreatmentFacilitiesFunctor;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

@TestInstance(Lifecycle.PER_CLASS)
public class RedisFacilityFindTests {

  private RedisFacilityDao dao;
  private RedisCategoryCodesDao _categoryCodesDao;
  private RedisServiceCodeDao _serviceCodesDao;
  private IRedisConnectionPool _redisConnectionPool;
  private LoadCategoriesAndServicesFunctor loadCategoriesAndServicesFunctor;
  private LoadTreatmentFacilitiesFunctor loadTreatmentFacilitiesFunctor;

  @BeforeAll
  private void setup() throws IOException {
    final Injector injector = Guice
        .createInjector(new RedisClientModule(new RedisConfig("127.0.0.1", 6379, 2)));
    dao = injector.getInstance(RedisFacilityDao.class);
    _categoryCodesDao = injector.getInstance(RedisCategoryCodesDao.class);
    _serviceCodesDao = injector.getInstance(RedisServiceCodeDao.class);
    _redisConnectionPool = injector.getInstance(IRedisConnectionPool.class);
    this.loadCategoriesAndServicesFunctor = injector.getInstance(LoadCategoriesAndServicesFunctor.class);
    this.loadTreatmentFacilitiesFunctor = injector.getInstance(LoadTreatmentFacilitiesFunctor.class);

    loadFixtures();
  }

  private void loadFixtures() throws IOException {
    this.loadCategoriesAndServicesFunctor.loadStream(Resources.getResource("fixtures/service_codes_records.json").openStream());
    this.loadTreatmentFacilitiesFunctor.loadStream(Resources.getResource("fixtures/locations_geocoded.json").openStream());
  }


  @AfterAll
  private void teardown() throws Exception {
    _redisConnectionPool.borrowConnection().sync().flushdb();
    _redisConnectionPool.close();
  }


  @Test
  public void test() throws Exception {
    SearchRequest searchRequest = new SearchRequest();
    ServicesCondition servicesCondition = new ServicesCondition(ImmutableList.of("MALE"), MatchOperator.MUST);
    searchRequest.setServiceConditions(ImmutableList.of(servicesCondition));

    CompletionStage<SearchResults<Facility>> results  = dao.find(searchRequest, Page.page());

  }
}
