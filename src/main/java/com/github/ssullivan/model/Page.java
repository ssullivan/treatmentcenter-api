package com.github.ssullivan.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Page {

  public static Page page() {
    return new AutoValue_Page(0, 25);
  }

  @JsonCreator
  public static Page page(@JsonProperty("offset") int offset, @JsonProperty("size") int size) {
    return new AutoValue_Page(offset, size);
  }

  @JsonProperty("offset")
  public abstract int offset();

  @JsonProperty("size")
  public abstract int size();
}
