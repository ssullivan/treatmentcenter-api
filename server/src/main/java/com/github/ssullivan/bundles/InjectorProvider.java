package com.github.ssullivan.bundles;

import com.google.inject.Injector;
import io.dropwizard.Application;
import javax.inject.Provider;

public class InjectorProvider implements Provider<Injector> {

  private final Application application;

  public InjectorProvider(Application application) {
    this.application = application;
  }

  @Override
  public Injector get() {
    return InjectorRegistry.getInjector(this.application);
  }
}
