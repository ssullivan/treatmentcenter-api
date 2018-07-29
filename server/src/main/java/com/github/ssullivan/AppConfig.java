package com.github.ssullivan;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import javax.annotation.Nullable;

public class AppConfig extends Configuration {

  private ElasticConfig elasticConfig;
  private RedisConfig redisConfig;
  private SwaggerBundleConfiguration swaggerBundleConfiguration;

  public AppConfig() {

  }

  @JsonProperty("swagger")
  @Nullable
  public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
    return swaggerBundleConfiguration;
  }

  public void setSwaggerBundleConfiguration(
      SwaggerBundleConfiguration swaggerBundleConfiguration) {
    this.swaggerBundleConfiguration = swaggerBundleConfiguration;
  }

  @JsonProperty("elasticsearch")
  @Nullable
  public ElasticConfig getElasticConfig() {
    return elasticConfig;
  }

  public void setElasticConfig(ElasticConfig elasticConfig) {
    this.elasticConfig = elasticConfig;
  }

  @JsonProperty("redis")
  @Nullable
  public RedisConfig getRedisConfig() {
    return redisConfig;
  }

  public void setRedisConfig(RedisConfig redisConfig) {
    this.redisConfig = redisConfig;
  }
}
