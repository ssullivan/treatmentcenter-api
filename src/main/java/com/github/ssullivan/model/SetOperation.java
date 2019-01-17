package com.github.ssullivan.model;

public enum SetOperation {
  UNION,
  INTERSECTION;

  public static SetOperation fromBooleanOp(final String booleanOp) {
    if (booleanOp == null) {
      return INTERSECTION;
    }
    switch (booleanOp) {
      case "AND":
      case "and":
        return INTERSECTION;
      case "OR":
      case "or":
        return UNION;
      default:
        return INTERSECTION;
    }
  }
}
