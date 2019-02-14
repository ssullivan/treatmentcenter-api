package com.github.ssullivan.model.datafeeds;

import com.github.ssullivan.model.Category;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Service;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable object representing all of the data from the SAMSHA locator spreadsheet.
 */
public class SamshaLocatorData implements Serializable {

  private final String feedId;
  private final Collection<Category> categories;
  private final Collection<Service> services;
  private final List<Facility> facilities;

  public SamshaLocatorData(
      final String feedId,
      final Collection<Category> categories,
      final Collection<Service> services,
      final List<Facility> facilities) {
    this.feedId = Objects.requireNonNull(feedId, "FeedId must not be null");
    this.categories = Objects.requireNonNull(categories, "Cats must not be null");
    this.services = Objects.requireNonNull(services, "Services must not be null");
    this.facilities = Objects.requireNonNull(facilities, "Facilities must not be null");
  }

  public Collection<Category> getCategories() {
    return Collections.unmodifiableCollection(categories);
  }

  public Collection<Service> getServices() {
    return Collections.unmodifiableCollection(services);
  }

  public List<Facility> getFacilities() {
    return Collections.unmodifiableList(facilities);
  }

  public String getFeedId() {
    return feedId;
  }

  /**
   * Simple checks to see if this data had anything we can use.
   *
   * @return true if the data *looks* good, or false otherwise
   */
  public boolean isGood() {
    return this.categories != null && !this.categories.isEmpty()
        && this.services != null
        && !this.services.isEmpty()
        && this.facilities != null
        && !this.facilities.isEmpty();
  }
}
