package com.github.ssullivan;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

public class AppConfig extends Configuration {

  private ElasticConfig elasticConfig;

  public AppConfig() {
    this.elasticConfig = new ElasticConfig();
  }

  @JsonProperty("elasticsearch")
  public ElasticConfig getElasticConfig() {
    return elasticConfig;
  }

  public void setElasticConfig(ElasticConfig elasticConfig) {
    this.elasticConfig = elasticConfig;
  }
}
