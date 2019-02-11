package com.github.ssullivan.model;

import io.lettuce.core.GeoArgs;
import io.lettuce.core.GeoArgs.Unit;

public enum GeoUnit {

  MILE("mi", 0.621371), // miles per km
  KILOMETER("km", 1.0), // km per km
  METER("m", 1000.0), // meters per km
  FEET("ft", 3280.84); // feet per km


  private String abbrev;
  private double scale;


  GeoUnit(String abbrev, double scale) {
    this.abbrev = abbrev;
    this.scale = scale;
  }

  public static GeoUnit asGeoUnit(final String unit) {
    switch (unit) {
      case "mi":
        return MILE;
      case "km":
        return KILOMETER;
      case "m":
        return METER;
      case "ft":
        return FEET;
      default:
        return MILE;
    }
  }

  public double convertTo(final GeoUnit toUnit, final double value) {
    if (this == toUnit) {
      return value;
    }

    double tempValue = value;
    if (this != GeoUnit.KILOMETER) {
      tempValue = tempValue / this.scale;
    }
    if (toUnit != GeoUnit.KILOMETER) {
      tempValue *= toUnit.scale; // Convert to destination unit.
    }
    return tempValue;
  }

  public GeoArgs.Unit unit() {
    switch (this) {
      case FEET:
        return Unit.ft;
      case MILE:
        return Unit.mi;
      case METER:
        return Unit.m;
      case KILOMETER:
        return Unit.km;
      default:
        return Unit.mi;
    }
  }

  public String getAbbrev() {
    return this.abbrev;
  }

  @Override
  public String toString() {
    return getAbbrev();
  }


}
