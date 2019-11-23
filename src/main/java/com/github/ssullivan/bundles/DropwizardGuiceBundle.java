package com.github.ssullivan.bundles;

import com.github.ssullivan.guice.DropwizardAwareModule;
import com.github.ssullivan.guice.IHealthcheckProvider;
import com.github.ssullivan.guice.IManagedProvider;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.servlet.GuiceFilter;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.EnumSet;
import javax.servlet.DispatcherType;

/**
 * Configures dropwizard with the Guice Hk2 bridge.
 *
 * @param <T> the type of dropwizard configuration
 */
public class DropwizardGuiceBundle<T extends Configuration> implements ConfiguredBundle<T> {

  private ImmutableList<Module> modules;
  private Application application;

  /**
   * Create a new instance of {@link DropwizardGuiceBundle}.
   *
   * @param moduleList the modules to configure for injection
   */
  public DropwizardGuiceBundle(final Module... moduleList) {
    this.modules = moduleList != null
        ? ImmutableList.copyOf(moduleList) : ImmutableList.of();

  }

  @Override
  public void run(T configuration, Environment environment) {

    // Configure the Guice servlet filter for cases where we want to use
    // the guice ServletModule
    environment.servlets()
        .addFilter(GuiceFilter.class.getName(), GuiceFilter.class)
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.ASYNC, DispatcherType.REQUEST),
            false, "/*");

    // Register our custom GuiceHk2BridgeFeature
    environment.jersey().register(new GuiceHk2BridgeFeature(
        new InjectorProvider(this.application)));

    // Create our injector for use by the application
    InjectorRegistry.registerInjector(this.application,
        createInjector(configuration, environment));

    InjectorRegistry.getInjector(this.application)
        .getInstance(IManagedProvider.class)
        .get()
        .forEach(managed -> environment.lifecycle().manage(managed));

    InjectorRegistry.getInjector(this.application)
        .getInstance(IHealthcheckProvider.class)
        .get()
        .forEach(healthCheck -> environment.healthChecks()
            .register(healthCheck.getClass().getName(), healthCheck));

  }

  /**
   * Create an instance of a Guice injector with the provided configuration, and environment.
   *
   * @param configuration the configuration for the dropwizard application
   * @param environment the environment for the dropwizard application
   * @return the injector
   */
  private Injector createInjector(final T configuration, final Environment environment) {

    for (Module module : modules) {
      if (module instanceof DropwizardAwareModule) {
        ((DropwizardAwareModule) module).setConfiguration(configuration);
        ((DropwizardAwareModule) module).setEnvironment(environment);
      }
    }

    return Guice.createInjector(new ImmutableList.Builder<Module>()
        .addAll(this.modules)
        .add(new DropwizardModule<>(configuration, environment))
        .build()
    );
  }

  @Override
  public void initialize(final Bootstrap<?> bootstrap) {
    application = bootstrap.getApplication();
  }
}
