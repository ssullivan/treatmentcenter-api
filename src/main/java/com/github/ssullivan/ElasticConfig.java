package com.github.ssullivan;

public class ElasticConfig {

  private String hosts;
  private String clustername;

  public String getClustername() {
    return clustername;
  }

  public void setClustername(String clustername) {
    this.clustername = clustername;
  }

  public String getHosts() {
    return hosts;
  }

  public void setHosts(String hosts) {
    this.hosts = hosts;
  }
}
