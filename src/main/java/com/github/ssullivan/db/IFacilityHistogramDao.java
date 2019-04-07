package com.github.ssullivan.db;

import java.io.IOException;
import java.util.Map;

public interface IFacilityHistogramDao {
  Map<String, Integer> toServicesHistogram(final String groupBy) throws IOException;
}
