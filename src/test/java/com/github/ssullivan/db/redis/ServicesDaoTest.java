package com.github.ssullivan.db.redis;

import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.db.redis.RedisServiceCodeDao;
import com.github.ssullivan.guice.RedisClientModule;
import com.github.ssullivan.model.Service;
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
public class ServicesDaoTest {

  private RedisServiceCodeDao _dao;
  private IRedisConnectionPool _pool;

  @BeforeAll
  private void setup() {
    final Injector injector = Guice
        .createInjector(new RedisClientModule(new RedisConfig("127.0.0.1", 6379)));
    _dao = injector.getInstance(RedisServiceCodeDao.class);
    _pool = injector.getInstance(IRedisConnectionPool.class);
  }

  @AfterAll
  private void cleanup() {
    _pool.close();
  }

  @Test
  public void testAddingService() throws IOException {
    Service service = new Service();
    service.setCode("TEST" + System.currentTimeMillis());
    service.setName("A test service");
    service.setCategoryCode("TEST");
    service.setDescription("A test service");

    final boolean wasadded = _dao.addService(service);

    MatcherAssert.assertThat(wasadded, Matchers.equalTo(true));

    _dao.delete(service.getCode());
  }

  @Test
  public void testFetchService() throws IOException {
    Service service = new Service();
    service.setCode("TEST2");
    service.setName("A test service");
    service.setCategoryCode("TEST");
    service.setDescription("A test service");

    final boolean wasadded = _dao.addService(service);
    final Service fromDb = _dao.get(service.getCode());

    MatcherAssert.assertThat(fromDb.getCode(), Matchers.equalTo(service.getCode()));
    MatcherAssert.assertThat(fromDb.getName(), Matchers.equalTo(service.getName()));
    MatcherAssert.assertThat(fromDb.getCategoryCode(), Matchers.equalTo(service.getCategoryCode()));
    MatcherAssert.assertThat(fromDb.getDescription(), Matchers.equalTo(service.getDescription()));


    _dao.delete(service.getCode());
  }
}
