package com.github.ssullivan.guice;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.github.ssullivan.SecretManagerConfig;
import com.google.inject.AbstractModule;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Guice module for setting up an instance of {@link AWSSecretsManager}
 */
public class AwsSecretManagerModule extends AbstractModule {
  private String region;
  private String endpoint;

  public AwsSecretManagerModule() {
  }

  public AwsSecretManagerModule(String region, String endpoint) {
    this.region = region;
    this.endpoint = endpoint;
  }

  @Override
  protected void configure() {
    bindConstant().annotatedWith(AwsSecretRegion.class).to(region);
    bindConstant().annotatedWith(AwsSecretEndpoint.class).to(endpoint);
    bind(ISecretProvider.class).to(AwsSecretProvider.class);
  }

  @Inject
  @Singleton
  AWSSecretsManager providesSecretsManager(@AwsSecretRegion String region,
      @AwsSecretRegion String endpoint) {

    AwsClientBuilder.EndpointConfiguration config = new EndpointConfiguration(endpoint, region);
    AWSSecretsManagerClientBuilder builder = AWSSecretsManagerClientBuilder.standard()
        .withEndpointConfiguration(config);

    return builder.build();
  }

  public AwsSecretManagerModule wtihConfig(SecretManagerConfig config) {
    Objects.requireNonNull(config, "config must not be null");
    this.region = config.getRegion();
    this.endpoint = config.getEndpoint();
    return this;
  }

  public AwsSecretManagerModule withRegion(final String region) {
    Objects.requireNonNull(region, "region must not be null");
    this.region = region;
    return this;
  }

  public AwsSecretManagerModule withEndpoint(final String endpoint) {
    Objects.requireNonNull(endpoint, "endpoint must not be null");
    this.endpoint = endpoint;
    return this;
  }
}
