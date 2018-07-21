package com.github.ssullivan;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

import javax.annotation.Nullable;

public class AppConfig extends Configuration {

  private ElasticConfig elasticConfig;
  private RedisConfig redisConfig;

  public AppConfig() {
    this.elasticConfig = new ElasticConfig();
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
