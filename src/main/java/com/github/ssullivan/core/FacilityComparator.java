package com.github.ssullivan.core;

import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.FacilityWithRadius;
import com.github.ssullivan.model.SortDirection;
import java.util.Comparator;
import java.util.Objects;

public class FacilityComparator<F extends Facility> implements Comparator<Facility> {

  private final String sortField;
  private final SortDirection sortDirection;

  public FacilityComparator(final String sortField,
      final SortDirection sortDirection) {
    this.sortField = sortField;
    this.sortDirection = sortDirection;
  }

  private int compareByScore(final Facility f1, final Facility f2) {
    return Double.compare(f1.getScore(), f2.getScore());
  }

  private int compareByRadius(final Facility f1, final Facility f2) {
    if (f1 instanceof FacilityWithRadius && f2 instanceof FacilityWithRadius) {
      return Double.compare(((FacilityWithRadius) f1).getRadius(), ((FacilityWithRadius) f2).getRadius());
    }
    else if (f1 instanceof FacilityWithRadius) {
      return 1;
    }
    else if (f2 instanceof FacilityWithRadius) {
      return -1;
    }
    return 0;
  }

  private int compareByString(final String f1, final String f2) {
    if (f1 == null && f2 == null) {
      return 0;
    }
    else if (f1 != null && f2 == null) {
      return 1;
    }
    else if (f1 == null) {
      return -1;
    }
    else {
      return f1.compareTo(f2);
    }
  }

  private int sortDir(int compareResult) {
    switch (sortDirection) {
      case ASC:
        return compareResult;
      case DESC:
        return -1 * compareResult;
    }
    return compareResult;
  }

  private int doCompare(final Facility f1, final Facility f2) {
    switch (sortField) {
      case "score":
        return compareByScore(f1, f2);
      case "radius":
        return compareByRadius(f1, f2);
      case "zip":
        return compareByString(f1.getZip(), f2.getZip());
      case "state":
        return compareByString(f1.getState(), f2.getState());
      case "name1":
        return compareByString(f1.getName1(), f2.getName2());
      case "name2":
        return compareByString(f1.getName2(), f2.getName2());
      case "city":
        return compareByString(f1.getCity(), f2.getCity());
      default:
        return compareByString(f1.getName1(), f2.getName1());
    }
  }

  @Override
  public int compare(final Facility f1, final Facility f2) {
    if (f1 == null && f2 == null) {
      return 0;
    }
    if (f1 == null) {
      return -1;
    }
    if (f2 == null) {
      return 1;
    }

    return sortDir(doCompare(f1, f2));
  }
}
