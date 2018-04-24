package com.github.ssullivan.bundles;

import com.google.inject.Injector;
import io.dropwizard.Application;
import java.util.concurrent.ConcurrentHashMap;

public final class InjectorRegistry {

  private ConcurrentHashMap<Application, Injector> injectorRegistry;

  private InjectorRegistry() {
    this.injectorRegistry = new ConcurrentHashMap<>();
  }

  public static void registerInjector(final Application application, final Injector injector) {
    InjectorRegistryHolder.INJECTOR_REGISTRY.injectorRegistry
        .put(application, injector);
  }

  public static Injector getInjector(final Application application) {
    return InjectorRegistryHolder.INJECTOR_REGISTRY.injectorRegistry
        .get(application);
  }

  private static class InjectorRegistryHolder {

    private static InjectorRegistry INJECTOR_REGISTRY = new InjectorRegistry();
  }
}
