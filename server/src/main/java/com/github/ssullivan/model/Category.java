package com.github.ssullivan.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

public class Category {
  private String code;
  private String name;
  private Set<String> serviceCodes;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<String> getServiceCodes() {
    return new HashSet<>(serviceCodes);
  }

  public void setServiceCodes(Set<String> serviceCodes) {
    this.serviceCodes = new HashSet<>(serviceCodes);
  }

  public void addServiceCode(@Nonnull final Service service) {
    this.serviceCodes.add(service.getCode());
  }

  public void addAllServiceCodes(@Nonnull final Service... services) {
    Stream.of(services)
        .filter(Objects::nonNull)
        .forEach(this::addServiceCode);
  }

  public void addAllServiceCodes(@Nonnull final Collection<Service> services) {
      services
          .stream()
          .filter(Objects::nonNull)
          .forEach(this::addServiceCode);
  }
}
