package com.github.ssullivan;

import java.util.Objects;
import javax.inject.Inject;

public class SecretManagerConfig {
  private String region;
  private String endpoint;

  @Inject
  public SecretManagerConfig(String region, String endpoint) {
    this.region = region;
    this.endpoint = endpoint;
  }

  public SecretManagerConfig() {
    region = "us-east-1";
    endpoint = "https://secretsmanager." + region + ".amazonaws.com";
  }

  public String getRegion() {
    return region;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public SecretManagerConfig withRegion(String region) {
    Objects.requireNonNull("region must not be null");
    this.region = region;
    return this;
  }

  public SecretManagerConfig withEndpoint(String endpoint) {
    Objects.requireNonNull("endpoint must not be null");
    this.endpoint = endpoint;
    return this;
  }
}
