package com.github.ssullivan;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RedisConfig {

  private String host;
  private int port;
  private int db;

  public RedisConfig() {
    this.host = "localhost";
    this.port = 6379;
    this.db = 0;
  }

  public RedisConfig(String host, int port) {
    this.host = host;
    this.port = port;
    this.db = 0;
  }

  public RedisConfig(String host, int port, int db) {
    this.host = host;
    this.port = port;
    this.db = db;
  }

  @JsonProperty("host")
  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  @JsonProperty("port")
  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  @JsonProperty("db")
  public int getDb() {
    return db;
  }

  public void setDb(int db) {
    this.db = db;
  }
}
