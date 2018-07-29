package com.github.ssullivan.bundles;

import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import javax.inject.Provider;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.ServiceLocatorProvider;

public class GuiceHk2BridgeFeature implements Feature, Provider<ServiceLocator> {

  private final Provider<Injector> injectorProvider;
  private ServiceLocator serviceLocator;

  /**
   * Creates a new instance of GuiceHk2BridgeFeature.
   *
   * @param injectorProvider the guice injector provider.
   */
  public GuiceHk2BridgeFeature(Provider<Injector> injectorProvider) {
    this.injectorProvider = injectorProvider;
  }

  @Override
  public ServiceLocator get() {
    return Preconditions.checkNotNull(serviceLocator, "ServiceLocator has not been set yet");
  }

  @Override
  public boolean configure(FeatureContext context) {
    this.serviceLocator = ServiceLocatorProvider.getServiceLocator(context);

    final Injector injector = injectorProvider.get();

    new GuiceHk2BridgeSetup(serviceLocator, injector)
        .setup();

    return true;
  }
}
