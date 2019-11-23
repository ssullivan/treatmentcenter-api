package com.github.ssullivan;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This model object is used for configuring instance of {@link io.lettuce.core.RedisClient}.
 */
public class RedisConfig {

  private String host;
  private int port;
  private int db;
  private long timeout;

  /**
   * Creates a new instance of {@link RedisConfig}. The hostname is localhost, the port is 6379 and
   * the database is 0
   */
  public RedisConfig() {
    this("localhost", 6379, 0);
  }

  public RedisConfig(final String host, final int port) {
    this(host, port, 0);
  }

  public RedisConfig(final String host, final int port, final int db) {
    this.host = host;
    this.port = port;
    this.db = db;
    this.timeout = 500;
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

  @JsonProperty("timeout")
  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }
}
