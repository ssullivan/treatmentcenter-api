package com.github.ssullivan.model.conditions;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.ssullivan.json.RangeConditionDeserializer;
import com.google.common.collect.Range;
import java.util.Objects;
import java.util.Optional;

@JsonDeserialize(using = RangeConditionDeserializer.class)
public class RangeCondition {
  private Optional<Integer> start;
  private Optional<Integer> stop;

  public RangeCondition(Integer start, Integer stop) {
    Objects.requireNonNull(start, "start must not be null");
    Objects.requireNonNull(stop, "stop must not be null");

    int compare = Integer.compare(start, stop);
    if (compare >= 1) {
      throw new IllegalArgumentException("start must be before stop");
    }

    this.start = Optional.of(start);
    this.stop = Optional.of(stop);
  }

  private RangeCondition() {
    this.start = Optional.empty();
    this.stop = Optional.empty();
  }

  public Optional<Integer> getStart() {
    return start;
  }

  public Optional<Integer> getStop() {
    return stop;
  }

  public boolean isUndefined() {
    return !this.start.isPresent() && !this.stop.isPresent();
  }

  public static RangeCondition lessThanOrEqual(Integer value) {
    RangeCondition toReturn = new RangeCondition();
    toReturn.stop = Optional.ofNullable(value);
    return toReturn;
  }

  public static RangeCondition greaterThanOrEqual(Integer value) {
    RangeCondition toReturn = new RangeCondition();
    toReturn.start = Optional.ofNullable(value);
    return toReturn;
  }

  public Range<Integer> toRange() {
    if (start.isPresent() && stop.isPresent()) {
      return Range.closed(start.get(), stop.get());
    }
    else
      return start.map(Range::atLeast)
          .orElseGet(() -> stop.map(Range::atMost).orElseGet(Range::all));
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RangeCondition that = (RangeCondition) o;
    return Objects.equals(getStart(), that.getStart()) &&
        Objects.equals(getStop(), that.getStop());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getStart(), getStop());
  }
}
