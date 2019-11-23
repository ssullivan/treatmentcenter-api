package com.github.ssullivan.lifecycle;

import com.zaxxer.hikari.HikariDataSource;
import io.dropwizard.lifecycle.Managed;
import javax.inject.Inject;

public class HikariDataSourceLifecycle implements Managed {

  private HikariDataSource hikariDataSource;

  @Inject
  public HikariDataSourceLifecycle(HikariDataSource hikariDataSource) {
    this.hikariDataSource = hikariDataSource;
  }

  @Override
  public void start() throws Exception {

  }

  @Override
  public void stop() throws Exception {
    this.hikariDataSource.close();
  }
}
