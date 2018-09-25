package com.github.ssullivan.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class Service {
  private String code;
  private String name;
  private String description;
  private String categoryCode;

  public Service() {
  }

  /**
   * Creates a new instance of {@link Service}.
   *
   * @param code the unique code for this service
   * @param name the name of this service
   * @param description a brief description of this service
   * @param categoryCode the category this service belongs too
   */
  public Service(String code, String name, String description, String categoryCode) {
    this.code = code;
    this.name = name;
    this.description = description;
    this.categoryCode = categoryCode;
  }

  @ApiModelProperty(value = "The code of the category that this service belongs in", example = "EDU")
  public String getCategoryCode() {
    return categoryCode;
  }

  public void setCategoryCode(String categoryCode) {
    this.categoryCode = categoryCode;
  }

  @ApiModelProperty(value = "The unique code for this service", example = "FCO")
  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  @ApiModelProperty(example = "Training and Education")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
