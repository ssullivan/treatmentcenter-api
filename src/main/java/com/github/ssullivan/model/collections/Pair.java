package com.github.ssullivan.model.collections;

public class Pair<L,R> {
  private L lhs;
  private R rhs;

  public Pair(L lhs, R rhs) {
    this.lhs = lhs;
    this.rhs = rhs;
  }

  public L getLhs() {
    return lhs;
  }

  public R getRhs() {
    return rhs;
  }
}
