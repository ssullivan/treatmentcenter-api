package com.github.ssullivan.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;

public abstract class DropwizardAwareModule<C extends Configuration> extends
    AbstractModule implements
    Module {

  private C configuration;
  private Environment environment;


  public C getConfiguration() {
    return configuration;
  }

  public void setConfiguration(C configuration) {
    this.configuration = configuration;
  }

  public Environment getEnvironment() {
    return environment;
  }

  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }
}
