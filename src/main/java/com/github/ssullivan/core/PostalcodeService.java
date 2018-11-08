package com.github.ssullivan.core;

import com.github.ssullivan.api.IPostalcodeService;
import com.github.ssullivan.model.GeoPoint;
import com.github.ssullivan.tasks.LoadZipCodesAndGeos;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public class PostalcodeService implements IPostalcodeService {

  private ImmutableMap<String, List<GeoPoint>> postalCodes;

  public PostalcodeService() {
    this.postalCodes = ImmutableMap.of();
  }

  @Override
  public ImmutableList<GeoPoint> fetchGeos(String postalCode) {
    return ImmutableList.copyOf(this.postalCodes.getOrDefault(postalCode,
        ImmutableList.of()));
  }

  @Override
  public void loadPostalCodes(File file) throws IOException {
    LoadZipCodesAndGeos loader = new LoadZipCodesAndGeos();
    postalCodes = loader.parse(file);
  }

  @Override
  public void loadPostalCodes(URI uri) {
    LoadZipCodesAndGeos loader = new LoadZipCodesAndGeos();

    // TODO: Implement this
  }
}
