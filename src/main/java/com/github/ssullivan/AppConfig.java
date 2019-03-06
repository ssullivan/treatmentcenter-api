package com.github.ssullivan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import javax.annotation.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig extends Configuration {

  private RedisConfig redisConfig;
  private SwaggerBundleConfiguration swaggerBundleConfiguration;
  private RdsConfig rdsConfig;


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


  @JsonProperty("redis")
  @Nullable
  public RedisConfig getRedisConfig() {
    return redisConfig;
  }

  public void setRedisConfig(RedisConfig redisConfig) {
    this.redisConfig = redisConfig;
  }

  @JsonProperty("postgres")
  @Nullable
  public RdsConfig getDatabaseConfig() {
    return rdsConfig;
  }

  public void setDatabaseConfig(RdsConfig databaseConfig) {
    this.rdsConfig = databaseConfig;
  }

  @JsonIgnore
  public boolean useRedis() {
    return redisConfig != null;
  }

  @JsonIgnore
  public boolean usePostgres() {
    return this.rdsConfig != null;
  }
}
