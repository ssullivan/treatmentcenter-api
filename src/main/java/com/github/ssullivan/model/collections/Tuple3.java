package com.github.ssullivan.model.collections;

import java.util.Objects;

public class Tuple3<T1,T2,T3> extends Tuple2<T1,T2> {
  private T3 t3;

  public Tuple3(T1 t1, T2 t2, T3 t3) {
    super(t1, t2);
    this.t3 = Objects.requireNonNull(t3);
  }

  public T3 get_3() {
    return t3;
  }
}
