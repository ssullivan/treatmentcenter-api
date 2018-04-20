package com.github.ssullivan.bundles;

import com.google.common.base.Preconditions;
import com.google.inject.Injector;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.guice.bridge.api.GuiceBridge;
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge;

/**
 * Utility class for lazily configuring the Guice+Hk2 bridge.
 */
public class GuiceHk2BridgeSetup {

  private final ServiceLocator serviceLocator;
  private final Injector injector;

  /**
   * Creates a new instance of {@link GuiceHk2BridgeSetup}.
   *
   * @param serviceLocator a non-null instance of the hk2 {@link ServiceLocator}
   * @param injector a non-null instance of the Guice {@link Injector}
   */
  public GuiceHk2BridgeSetup(final ServiceLocator serviceLocator, final Injector injector) {
    this.serviceLocator = Preconditions.checkNotNull(serviceLocator,
        "HK2 ServiceLocator must not be null");
    this.injector = Preconditions.checkNotNull(injector,
        "Guice injector must not be null");
  }

  /**
   * Configures a bi-directional bridge between Guice, and HK2.
   */
  public void setup() {
    GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator);
    GuiceIntoHK2Bridge g2h = serviceLocator.getService(GuiceIntoHK2Bridge.class);
    g2h.bridgeGuiceInjector(injector);
  }
}
