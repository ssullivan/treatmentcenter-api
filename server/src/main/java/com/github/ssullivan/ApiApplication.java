package com.github.ssullivan;

import com.github.ssullivan.bundles.DropwizardGuiceBundle;
import com.github.ssullivan.guice.DropwizardAwareModule;
import com.github.ssullivan.guice.ElasticClientModule;
import com.google.common.collect.ImmutableMultimap;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.io.PrintWriter;

public class ApiApplication extends Application<AppConfig> {

  public static void main(String[] args) throws Exception {
    new ApiApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<AppConfig> bootstrap) {
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
            new EnvironmentVariableSubstitutor(false)
        )
    );

    final DropwizardAwareModule<AppConfig> module = new DropwizardAwareModule<AppConfig>() {
      @Override
      protected void configure() {
        install(new ElasticClientModule(getConfiguration().getElasticConfig()));
      }
    };

    bootstrap.addBundle(new DropwizardGuiceBundle<>(module));
  }

  @Override
  public void run(AppConfig configuration, Environment environment) throws Exception {
    environment.jersey().packages(this.getClass().getPackage().getName());
  }
}
