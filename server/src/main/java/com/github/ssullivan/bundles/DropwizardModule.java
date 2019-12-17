package com.github.ssullivan.bundles;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;

/**
 * Binds specific Dropwizard related objects (configuration, environment) so they are available for
 * injection.
 *
 * @param <T> the type of configuration
 */
public class DropwizardModule<T extends Configuration> extends AbstractModule {

  private final T configuration;
  private final Environment environment;

  /**
   * Creates a new instance of {@link DropwizardModule}.
   * This module will bind the configuratoin, and environment so they
   * are available for injection.
   *
   * @param configuration the applications configuration
   * @param environment the applications environment
   */
  public DropwizardModule(T configuration, Environment environment) {
    this.configuration = Preconditions
        .checkNotNull(configuration, "Configuration must not be null");
    this.environment = Preconditions.checkNotNull(environment, "Environment must not be null");
  }

  @Override
  protected void configure() {
    bindConfiguration();
    bind(Configuration.class).toInstance(this.configuration);
    bind(Environment.class).toInstance(this.environment);
    bind(ObjectMapper.class).toInstance(this.environment.getObjectMapper());
  }

  private void bindConfiguration() {
    final Class type = this.configuration.getClass();

    bind(type, this.configuration);

    if (type == Configuration.class) {
      return;
    }

    bind(type.getSuperclass(), this.configuration);
  }

  @SuppressWarnings("unchecked")
  private void bind(final Class type, final Object instance) {
    bind(type).toInstance(instance);
  }
}
