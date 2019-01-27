package com.github.ssullivan.model.datafeeds;

import com.github.ssullivan.model.Category;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Service;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

public class SamshaLocatorData implements Serializable {
  private final Collection<Category> categories;
  private final Collection<Service> services;
  private final Collection<Facility> facilities;

  public SamshaLocatorData(final Collection<Category> categories,
      final Collection<Service> services,
      final Collection<Facility> facilities) {
    this.categories = categories;
    this.services = services;
    this.facilities = facilities;
  }

  public Collection<Category> getCategories() {
    return Collections.unmodifiableCollection(categories);
  }

  public Collection<Service> getServices() {
    return Collections.unmodifiableCollection(services);
  }

  public Collection<Facility> getFacilities() {
    return Collections.unmodifiableCollection(facilities);
  }
}
