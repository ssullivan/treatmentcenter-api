package com.github.ssullivan.model;

import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

@ApiModel
public class Category {

  private String code;
  private String name;
  private Set<String> serviceCodes;
  private Set<Service> services;

  public Category() {
    this.serviceCodes = new HashSet<>();
    this.services = new HashSet<>();
  }

  public Category(final String code, final String name, final Set<String> serviceCodes) {
    this.code = code;
    this.name = name;
    this.serviceCodes = new HashSet<>(serviceCodes);
    this.services = new HashSet<>();
  }

  public Category(final String code, final String name, final Collection<Service> services) {
    this(code, name, services.stream().map(Service::getCode).collect(Collectors.toSet()));
    this.services = new HashSet<>(services);
  }


  @ApiModelProperty(value = "Unique code for this category", example = "EDU")
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  @ApiModelProperty(example = "Counseling Services and Education")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @ApiModelProperty(value = "A list of service codes", example = "[ABC,EDU]", dataType = "java.util.Set")
  public Set<String> getServiceCodes() {
    return new HashSet<>(serviceCodes);
  }

  public void setServiceCodes(Set<String> serviceCodes) {
    this.serviceCodes = new HashSet<>(serviceCodes);
  }

  public void addServiceCode(@Nonnull final Service service) {
    this.services.add(service);
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


  @ApiModelProperty(value = "A list of services", example = "[ABC,EDU]", dataType = "java.util.Set")
  public Set<Service> getServices() {
    if (this.services == null) {
      return ImmutableSet.of();
    }

    return ImmutableSet.copyOf(services);
  }

  public void setServices(Set<Service> services) {
    if (services == null) {
      return;
    }

    this.services = ImmutableSet.copyOf(services);
  }
}
