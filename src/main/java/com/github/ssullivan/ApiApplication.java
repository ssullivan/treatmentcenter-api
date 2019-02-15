package com.github.ssullivan;

import com.github.ssullivan.api.IPostalcodeService;
import com.github.ssullivan.bundles.DropwizardGuiceBundle;
import com.github.ssullivan.bundles.InjectorRegistry;
import com.github.ssullivan.core.PostalcodeService;
import com.github.ssullivan.guice.DropwizardAwareModule;
import com.github.ssullivan.guice.PropPostalcodesPath;
import com.github.ssullivan.guice.RedisClientModule;
import com.github.ssullivan.healthchecks.RedisHealthCheck;
import com.github.ssullivan.tasks.LoadCategoriesAndServicesTask;
import com.github.ssullivan.tasks.LoadTreatmentFacilitiesTask;
import com.github.ssullivan.tasks.feeds.LoadSamshaCommand;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiApplication extends Application<AppConfig> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiApplication.class);

  public static void main(String[] args) throws Exception {
    new ApiApplication().run(args);
  }

  private static String getProperty(final String property, String defaultValue) {
    final String fromEnvs = System.getenv(property);
    final String fromProps = System.getProperty(property);

    if (fromEnvs != null) {
      return fromEnvs;
    }

    if (fromProps != null) {
      return fromProps;
    }
    return defaultValue;
  }

  @Override
  public void initialize(Bootstrap<AppConfig> bootstrap) {

    bootstrap.addCommand(new LoadSamshaCommand());
    bootstrap.addCommand(new LoadCategoriesAndServicesTask());
    bootstrap.addCommand(new LoadTreatmentFacilitiesTask());

    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
            new EnvironmentVariableSubstitutor(false)
        )
    );

    bootstrap.addBundle(new SwaggerBundle<AppConfig>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(AppConfig configuration) {
        SwaggerBundleConfiguration swaggerBundleConfiguration = configuration
            .getSwaggerBundleConfiguration();
        if (swaggerBundleConfiguration == null) {
          swaggerBundleConfiguration = new SwaggerBundleConfiguration();
        }
        swaggerBundleConfiguration.setSchemes(new String[]{"http", "https"});
        swaggerBundleConfiguration.setVersion(getProperty("API_VERSION", "dev"));
        swaggerBundleConfiguration.setIsPrettyPrint(true);

        final String environment = getProperty("ENVIRONMENT", "dev");
        if ("prod".equalsIgnoreCase(environment)) {
          swaggerBundleConfiguration.setHost("api.centerlocator.org");
        }

        swaggerBundleConfiguration.setTitle("Treatmentcenter API");
        swaggerBundleConfiguration
            .setDescription("An OpenAPI to find treatment centers for substance abuse");
        swaggerBundleConfiguration.setResourcePackage("com.github.ssullivan.resources");

        return swaggerBundleConfiguration;
      }
    });



    final DropwizardAwareModule<AppConfig> module = new DropwizardAwareModule<AppConfig>() {
      @Override
      protected void configure() {
        if (getConfiguration().getRedisConfig() != null) {
          install(new RedisClientModule(getConfiguration().getRedisConfig()));
          LOGGER.info("Configuring application to connect to Redis/ElatiCache");
        } else {
          LOGGER.info("No configuration provided for Redis/ElastiCache");
        }

        LOGGER.info("Attempting to get config from S3");

      }
    };

    bootstrap.addBundle(new DropwizardGuiceBundle<>(module,
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(IPostalcodeService.class).to(PostalcodeService.class).in(Singleton.class);
            bindConstant().annotatedWith(PropPostalcodesPath.class)
                .to(getProperty("POSTALCODES_US_PATH",
                    "/treatmentcenter-api-latest/data/US.txt"));
          }
        }));
  }

  private String getAllowedOrigins() {
    final String fromEnvs = System.getenv("CORS_ALLOWED_ORIGINS");
    final String fromProps = System.getProperty("CORS_ALLOWED_ORIGINS");

    if (fromEnvs != null) {
      return fromEnvs;
    }

    if (fromProps != null) {
      return fromProps;
    }

    return "^https?://[^.]+(?:.centerlocator.org|localhost|127.0.0.1)(:[0-9]+)?$";
  }

  @Override
  public void run(AppConfig configuration, Environment environment) throws Exception {


    final Injector injector = InjectorRegistry.getInjector(this);
    environment.healthChecks().register(RedisHealthCheck.class.getSimpleName(),
        injector.getInstance(RedisHealthCheck.class));
    environment.healthChecks().runHealthChecks()
        .forEach((s, result) -> {
          if (!result.isHealthy()) {
            LOGGER.error("HealthCheck {} is not healthy because {}", result.getMessage(), result.getError());
          }
        });

    environment.jersey().packages(this.getClass().getPackage().getName());
    FilterHolder filterHolder = environment.getApplicationContext()
        .addFilter(CrossOriginFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
    filterHolder.setAsyncSupported(true);
    filterHolder.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, getAllowedOrigins());
    filterHolder.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,HEAD,OPTIONS");
    filterHolder.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM,
        "X-Requested-With,Content-Type,Accept,Origin,Cache-Control");
  }
}
