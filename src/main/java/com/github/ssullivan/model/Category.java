package com.github.ssullivan.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

@ApiModel
public class Category {
  private String code;
  private String name;
  private Set<String> serviceCodes;

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
