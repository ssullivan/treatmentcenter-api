package com.github.ssullivan.api;

import com.github.ssullivan.core.PostalcodeService;
import com.github.ssullivan.model.GeoPoint;
import com.google.common.collect.ImmutableList;
import com.google.inject.ImplementedBy;
import java.io.File;
import java.io.IOException;
import java.net.URI;

@ImplementedBy(PostalcodeService.class)
public interface IPostalcodeService {

  /**
   * Fetch GeoPoints for the specified postalcode
   *
   * @param postcalCode the postalcode
   * @return a non-null list of GeoPoints
   */
  ImmutableList<GeoPoint> fetchGeos(final String postcalCode);


  /**
   * Load postalcodes tab separated file.
   *
   * @param file the path to the file to load
   * @throws IOException
   */
  void loadPostalCodes(final File file) throws IOException;

  void loadPostalCodes(final URI uri);
}
