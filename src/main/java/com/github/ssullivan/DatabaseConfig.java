package com.github.ssullivan;

public class DatabaseConfig {

  private String username = "postgres";
  private String password;
  private String databaseName;

  private String host = "localhost";
  private int port = 5432;
  private boolean ssl = false;

  public DatabaseConfig() {
    this.password = "";
  }

  public String getDatabaseName() {
    return databaseName;
  }

  public void setDatabaseName(String databaseName) {
    this.databaseName = databaseName;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public boolean getSsl() {
    return this.ssl;
  }

  public void setSsl(boolean ssl) {
    this.ssl = ssl;
  }

}
