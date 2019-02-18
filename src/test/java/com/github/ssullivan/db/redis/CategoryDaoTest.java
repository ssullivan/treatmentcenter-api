package com.github.ssullivan.db.redis;

import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.db.redis.RedisCategoryCodesDao;
import com.github.ssullivan.guice.RedisClientModule;
import com.github.ssullivan.model.Category;
import com.google.common.collect.ImmutableSet;
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
public class CategoryDaoTest {

  private RedisCategoryCodesDao dao;
  private IRedisConnectionPool pool;

  @BeforeAll
  private void setup() {
    final Injector injector = Guice
        .createInjector(new RedisClientModule(new RedisConfig("127.0.0.1", 6379)));
    dao = injector.getInstance(RedisCategoryCodesDao.class);
    pool = injector.getInstance(IRedisConnectionPool.class);
  }

  @AfterAll
  private void cleanup() {
    pool.close();
  }

  @Test
  public void testAddingCategoryCode() throws IOException {

    Category category = new Category();
    category.setCode("TEST" + System.currentTimeMillis());
    category.setName("A test category");
    category.setServiceCodes(ImmutableSet.of("FOO"));

    final boolean wasadded = dao.addCategory(category);
    MatcherAssert.assertThat(wasadded, Matchers.equalTo(true));
    dao.delete(category.getCode());
  }

  @Test
  public void testFetchingCategoryCode() throws IOException {
    Category category = new Category();
    category.setCode("TEST2");
    category.setName("A test category");
    category.setServiceCodes(ImmutableSet.of("FOO"));

    final boolean wasadded = dao.addCategory(category);
    final Category fromDb = dao.get(category.getCode());

    MatcherAssert.assertThat(fromDb.getCode(), Matchers.equalTo(category.getCode()));
    MatcherAssert.assertThat(fromDb.getName(), Matchers.equalTo(category.getName()));
    MatcherAssert.assertThat(fromDb.getServiceCodes(), Matchers.containsInAnyOrder("FOO"));

    dao.delete(category.getCode());
  }
}
