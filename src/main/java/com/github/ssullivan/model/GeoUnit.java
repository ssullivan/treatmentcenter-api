package com.github.ssullivan.model;

public enum GeoUnit {

  MILE(0.621371), // miles per km
  KILOMETER(1.0), // km per km
  METER(1000.0), // meters per km
  FEET(3280.84); // feet per km

  private double scale;


  GeoUnit(double scale) {
    this.scale = scale;
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

}
