package com.github.ssullivan.core;

import com.github.ssullivan.db.ICategoryCodesDao;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class AvailableServiceControllerTest {

  @Test
  public void testGuiceSetup() {
    final Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(ICategoryCodesDao.class).toInstance(Mockito.mock(ICategoryCodesDao.class));
      }
    });

    final IAvailableServiceController first = injector.getInstance(IAvailableServiceController.class);
    final IAvailableServiceController second = injector.getInstance(IAvailableServiceController.class);

    MatcherAssert.assertThat(first, Matchers.equalTo(second));
  }
}
