package com.github.ssullivan;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RdsConfig extends DatabaseConfig {

  private String region = "us-east-2";
  private boolean iamAuth;

  public RdsConfig() {
    iamAuth = false;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  @JsonProperty("authIAM")
  public boolean isIamAuth() {
    return iamAuth;
  }

  public void setIamAuth(boolean iamAuth) {
    this.iamAuth = iamAuth;
  }
}
