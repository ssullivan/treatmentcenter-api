package com.github.ssullivan.model;

public class Id<T> {
  private final T id;

  public Id(T id) {
    this.id = id;
  }

  public T getId() {
    return id;
  }
}
