package com.github.ssullivan.guice;

import com.github.ssullivan.ElasticConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.elasticsearch.client.RestHighLevelClient;

public class ElasticClientModule extends AbstractModule {

  public ElasticConfig config;

  public ElasticClientModule(ElasticConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(ElasticConfig.class).toInstance(config);
    bind(RestHighLevelClient.class).toProvider(ElasticRestClientProvider.class).in(Singleton.class);
  }
}
