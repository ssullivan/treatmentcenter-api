package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.RedisServiceCodeDao;
import com.github.ssullivan.model.Category;
import com.github.ssullivan.model.Service;
import com.github.ssullivan.resources.ServiceCodesResource;
import io.dropwizard.testing.junit.ResourceTestRule;
import java.io.IOException;
import java.util.List;
import javax.ws.rs.core.GenericType;
import org.assertj.core.util.Lists;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;

public class ServiceResourceTest {
  private static final GenericType<List<Service>> LIST_SERVICES
      = new GenericType<List<Service>>() {};

  private static final RedisServiceCodeDao dao = Mockito.mock(RedisServiceCodeDao.class);

  @ClassRule
  public static final ResourceTestRule resources = ResourceTestRule.builder()
      .addResource(new ServiceCodesResource(dao))
      .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
      .build();

  private final Service service = new Service("TEST", "Lorem Ipsum", "Test",
      "Test");

  @Before
  public void setup() throws IOException {
    Mockito.when(dao.get(Mockito.eq("TEST"))).thenReturn(service);
    Mockito.when(dao.listServices()).thenReturn(Lists.newArrayList(service));
  }

  @After
  public void teardown() {
    Mockito.reset(dao);
  }

  @Test
  public void testListServices() {
    final List<Service> services =
        resources.target("services").request().get(LIST_SERVICES);

    MatcherAssert.assertThat(services, Matchers.notNullValue());
    MatcherAssert.assertThat(services.size(), Matchers.equalTo(1));

    Service firstService = services.get(0);
    MatcherAssert.assertThat(firstService, Matchers.notNullValue());
    MatcherAssert.assertThat(firstService.getCode(), Matchers.equalTo(service.getCode()));
    MatcherAssert.assertThat(firstService.getCategoryCode(), Matchers.equalTo(service.getCategoryCode()));
    MatcherAssert.assertThat(firstService.getName(), Matchers.equalTo(service.getName()));
  }
}
