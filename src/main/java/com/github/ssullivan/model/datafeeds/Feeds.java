package com.github.ssullivan.model.datafeeds;

import java.util.ArrayList;
import java.util.List;

public class Feeds {
  private List<Feed> feeds;

  public Feeds() {

  }

  public Feeds(List<Feed> feeds) {
    if (feeds != null)
      this.feeds = new ArrayList<>(feeds);
  }

  public List<Feed> getFeeds() {
    return feeds;
  }

  public void setFeeds(List<Feed> feeds) {
    if (feeds != null)
      this.feeds = new ArrayList<>(feeds);
  }
}
