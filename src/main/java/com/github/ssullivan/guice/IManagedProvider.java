package com.github.ssullivan.guice;

import com.google.inject.ImplementedBy;
import io.dropwizard.lifecycle.Managed;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Provider;

@ImplementedBy(IManagedProvider.DefaultManagedProvider.class)
public interface IManagedProvider extends Provider<List<Managed>> {

  class DefaultManagedProvider implements IManagedProvider {

    @Override
    public List<Managed> get() {
      return new ArrayList<>();
    }
  }
}
