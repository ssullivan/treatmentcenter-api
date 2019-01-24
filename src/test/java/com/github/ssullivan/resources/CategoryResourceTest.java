package com.github.ssullivan.resources;

import com.github.ssullivan.db.redis.RedisCategoryCodesDao;
import com.github.ssullivan.model.Category;
import com.github.ssullivan.resources.CategoryCodesResource;
import com.google.common.collect.ImmutableSet;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import java.io.IOException;
import java.util.List;
import javax.ws.rs.core.GenericType;
import org.assertj.core.util.Lists;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;


@ExtendWith(DropwizardExtensionsSupport.class)
@TestInstance(Lifecycle.PER_CLASS)
public class CategoryResourceTest {

  private static final GenericType<List<Category>> LIST_CATEGORIES = new GenericType<List<Category>>() {
  };
  private static final RedisCategoryCodesDao dao = Mockito.mock(RedisCategoryCodesDao.class);

  public static final ResourceExtension resources = ResourceExtension.builder()
      .addResource(new CategoryCodesResource(dao))
      .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
      .build();

  private final Category category = new Category("TEST", "Lorem Ipsum",
      ImmutableSet.of("FOO"));

  @BeforeAll
  public void setup() throws IOException {
    Mockito.when(dao.get(Mockito.eq("TEST"))).thenReturn(category);
    Mockito.when(dao.listCategories()).thenReturn(Lists.newArrayList(category));
  }

  @AfterAll
  public void teardown() {
    Mockito.reset(dao);
  }

  @Test
  public void testListCategories() {
    final List<Category> categories =
        resources.target("categories").request().get(LIST_CATEGORIES);

    MatcherAssert.assertThat(categories, Matchers.notNullValue());
    MatcherAssert.assertThat(categories.size(), Matchers.equalTo(1));

    Category firstCategory = categories.get(0);
    MatcherAssert.assertThat(firstCategory, Matchers.notNullValue());
    MatcherAssert.assertThat(firstCategory.getCode(), Matchers.equalTo(category.getCode()));
    MatcherAssert.assertThat(firstCategory.getServiceCodes(), Matchers.containsInAnyOrder("FOO"));
    MatcherAssert.assertThat(firstCategory.getName(), Matchers.equalTo(category.getName()));
  }
}
