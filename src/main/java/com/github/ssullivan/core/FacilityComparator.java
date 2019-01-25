package com.github.ssullivan.core;

import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.FacilityWithRadius;
import java.util.Comparator;

public class FacilityComparator<F extends Facility> implements Comparator<Facility> {

  @Override
  public int compare(Facility f1, Facility f2) {
    if (f1 == null && f2 != null) {
      return -1;
    }
    else if (f1 != null && f2 == null) {
      return 1;
    }
    else if (f1 == null) {
      return 0;
    }

    int retval = Double.compare(f2.getScore(), f2.getScore());
    if (retval == 0 && f1 instanceof FacilityWithRadius && f2 instanceof FacilityWithRadius) {
      return Double.compare(((FacilityWithRadius) f1).getRadius(), ((FacilityWithRadius) f2).getRadius());
    }
    return retval;
  }
}
