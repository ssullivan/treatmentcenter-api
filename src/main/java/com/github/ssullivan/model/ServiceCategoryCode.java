package com.github.ssullivan.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceCategoryCode {

  private String categoryCode;
  private String categoryName;
  private String serviceCode;
  private String serviceName;
  private String serviceDescription;

  public ServiceCategoryCode() {

  }

  @JsonProperty("category_code")
  public String getCategoryCode() {
    return categoryCode;
  }

  public void setCategoryCode(String categoryCode) {
    this.categoryCode = categoryCode;
  }

  @JsonProperty("category_name")
  public String getCategoryName() {
    return categoryName;
  }

  public void setCategoryName(String categoryName) {
    this.categoryName = categoryName;
  }

  @JsonProperty("service_code")
  public String getServiceCode() {
    return serviceCode;
  }

  public void setServiceCode(String serviceCode) {
    this.serviceCode = serviceCode;
  }

  @JsonProperty("service_name")
  public String getServiceName() {
    return serviceName;
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  @JsonProperty("service_description")
  public String getServiceDescription() {
    return serviceDescription;
  }

  public void setServiceDescription(String serviceDescription) {
    this.serviceDescription = serviceDescription;
  }
}
