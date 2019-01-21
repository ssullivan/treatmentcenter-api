package com.github.ssullivan.model.datafeeds;

public class Feed {
  private String name;
  private String lastKey;
  private long lastTimestamp;

  public Feed() {
  }

  public Feed(String name, String lastKey, long lastTimestamp) {
    this.name = name;
    this.lastKey = lastKey;
    this.lastTimestamp = lastTimestamp;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLastKey() {
    return lastKey;
  }

  public void setLastKey(String lastKey) {
    this.lastKey = lastKey;
  }

  public long getLastTimestamp() {
    return lastTimestamp;
  }

  public void setLastTimestamp(long lastTimestamp) {
    this.lastTimestamp = lastTimestamp;
  }
}
