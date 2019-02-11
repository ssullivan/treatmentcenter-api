package com.github.ssullivan.model.collections;

import java.util.Objects;

public class Tuple2<T1, T2> {

  private T1 t1;
  private T2 t2;

  public Tuple2(T1 t1, T2 t2) {
    this.t1 = Objects.requireNonNull(t1);
    this.t2 = Objects.requireNonNull(t2);
  }

  public T1 get_1() {
    return t1;
  }

  public T2 get_2() {
    return t2;
  }

}
