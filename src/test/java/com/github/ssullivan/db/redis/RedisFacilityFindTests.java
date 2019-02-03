package com.github.ssullivan.db.redis;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.db.redis.RedisCategoryCodesDao;
import com.github.ssullivan.db.redis.RedisFacilityDao;
import com.github.ssullivan.db.redis.RedisServiceCodeDao;
import com.github.ssullivan.db.redis.RollingIdGenerator;
import com.github.ssullivan.db.redis.search.FindBySearchRequest;
import com.github.ssullivan.guice.RedisClientModule;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.GeoPoint;
import com.github.ssullivan.model.GeoRadiusCondition;
import com.github.ssullivan.model.GeoUnit;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
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
  private FindBySearchRequest findBySearchRequest;

  @BeforeAll
  private void setup() throws IOException {
    final Injector injector = Guice
        .createInjector(new RedisClientModule(new RedisConfig("127.0.0.1", 6379, 2)));
    dao = injector.getInstance(RedisFacilityDao.class);
    _categoryCodesDao = injector.getInstance(RedisCategoryCodesDao.class);
    _serviceCodesDao = injector.getInstance(RedisServiceCodeDao.class);
    _redisConnectionPool = injector.getInstance(IRedisConnectionPool.class);
    this.findBySearchRequest = injector.getInstance(FindBySearchRequest.class);
    this.loadCategoriesAndServicesFunctor = injector.getInstance(LoadCategoriesAndServicesFunctor.class);
    this.loadTreatmentFacilitiesFunctor = injector.getInstance(LoadTreatmentFacilitiesFunctor.class);

    IFeedDao feedDao = injector.getInstance(IFeedDao.class);
    feedDao.setCurrentFeedId(feedDao.nextFeedId().get());

    loadFixtures();
  }

  private void loadFixtures() throws IOException {
    this.loadCategoriesAndServicesFunctor.loadStream(Resources.getResource(
        "fixtures/service_codes_records.json").openStream());
    this.loadTreatmentFacilitiesFunctor.loadStream(Resources.getResource(
        "fixtures/locations_geocoded.json").openStream());
  }


  @AfterAll
  private void teardown() throws Exception {
    _redisConnectionPool.borrowConnection().sync().flushdb();
    _redisConnectionPool.close();
  }

  @Test
  public void testSearchingForMultipleServiceCodes_DifferentSets() throws Exception {
    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setServiceConditions(ImmutableList.of( new ServicesCondition(ImmutableList.of("BIA"), MatchOperator.MUST),
        new ServicesCondition(ImmutableList.of("SI"), MatchOperator.MUST)));

    final CompletableFuture<SearchResults<Facility>> promise  = findBySearchRequest.find(searchRequest, Page.page())
        .toCompletableFuture();

    Awaitility.await()
        .atMost(1, TimeUnit.DAYS)
        .pollInterval(10, TimeUnit.MILLISECONDS)
        .until(promise::isDone);

    final SearchResults<Facility> searchResults = promise.getNow(SearchResults.empty());
    MatcherAssert.assertThat(searchResults.hits(), hasSize(3));
    MatcherAssert.assertThat(searchResults.hits().get(0).getId(), equalTo(3L));
    MatcherAssert.assertThat(searchResults.hits().get(1).getId(), equalTo(5L));
    MatcherAssert.assertThat(searchResults.hits().get(2).getId(), equalTo(7L));
  }

  @Test
  public void testSearchingForMultipleServiceCodes_SameSets() throws Exception {
    SearchRequest searchRequest = new SearchRequest();
    searchRequest.setServiceConditions(ImmutableList.of( new ServicesCondition(ImmutableList.of("BIA", "SI"), MatchOperator.MUST)));

    final CompletableFuture<SearchResults<Facility>> promise  = findBySearchRequest.find(searchRequest, Page.page())
        .toCompletableFuture();

    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .pollInterval(10, TimeUnit.MILLISECONDS)
        .until(promise::isDone);

    final SearchResults<Facility> searchResults = promise.getNow(SearchResults.empty());
    MatcherAssert.assertThat(searchResults.hits(), hasSize(3));
    MatcherAssert.assertThat(searchResults.hits().get(0).getId(), equalTo(3L));
    MatcherAssert.assertThat(searchResults.hits().get(1).getId(), equalTo(5L));
    MatcherAssert.assertThat(searchResults.hits().get(2).getId(), equalTo(7L));
  }

  @Test
  public void testSearchingForASingleCondition() throws Exception {
    SearchRequest searchRequest = new SearchRequest();
    ServicesCondition servicesCondition = new ServicesCondition(ImmutableList.of("MALE"), MatchOperator.MUST);
    searchRequest.setServiceConditions(ImmutableList.of(servicesCondition));

    final CompletableFuture<SearchResults<Facility>> promise  = findBySearchRequest.find(searchRequest, Page.page())
        .toCompletableFuture();

    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .pollInterval(10, TimeUnit.MILLISECONDS)
        .until(promise::isDone);

    final SearchResults<Facility> searchResults = promise.getNow(SearchResults.empty());
    MatcherAssert.assertThat(searchResults.totalHits(),equalTo(8L));
    MatcherAssert.assertThat(searchResults.hits(), hasSize(8));

    final Facility firstFacility = searchResults.hits().get(0);
    MatcherAssert.assertThat(firstFacility.getId(), equalTo(1L));
    MatcherAssert.assertThat(firstFacility.getName1(), equalTo("Location 1"));
  }

  @Test
  public void testSearchingByGeoCondition() throws Exception {
    SearchRequest searchRequest = new SearchRequest();
    GeoRadiusCondition geoRadiusCondition = new GeoRadiusCondition(GeoPoint.geoPoint(38.80, -76.80), 15, GeoUnit.MILE);
    searchRequest.setGeoRadiusCondition(geoRadiusCondition);

    final CompletableFuture<SearchResults<Facility>> promise  = findBySearchRequest.find(searchRequest, Page.page())
        .toCompletableFuture();

    Awaitility.await()
        .atMost(1, TimeUnit.SECONDS)
        .pollInterval(10, TimeUnit.MILLISECONDS)
        .until(promise::isDone);

    final SearchResults<Facility> searchResults = promise.getNow(SearchResults.empty());
    MatcherAssert.assertThat(searchResults.totalHits(),equalTo(2L));
    MatcherAssert.assertThat(searchResults.hits(), hasSize(2));

    final Facility firstFacility = searchResults.hits().get(0);
    MatcherAssert.assertThat(firstFacility.getId(), equalTo(1L));
    MatcherAssert.assertThat(firstFacility.getName1(), equalTo("Location 1"));

    final Facility secondFacility = searchResults.hits().get(1);
    MatcherAssert.assertThat(secondFacility.getId(), equalTo(2L));
    MatcherAssert.assertThat(secondFacility.getName1(), equalTo("Location 2"));
  }
}
