package com.github.ssullivan.guice;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.ImplementedBy;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Provider;

@ImplementedBy(IHealthcheckProvider.DefaultHealthcheckProvider.class)
public interface IHealthcheckProvider extends Provider<List<HealthCheck>> {

  class DefaultHealthcheckProvider implements IHealthcheckProvider {

    @Override
    public List<HealthCheck> get() {
      return new ArrayList<>();
    }
  }
}
