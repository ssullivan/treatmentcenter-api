package com.github.ssullivan.core;

import com.github.ssullivan.api.IPostalcodeService;
import com.github.ssullivan.guice.PropPostalcodesPath;
import com.github.ssullivan.model.GeoPoint;
import com.github.ssullivan.tasks.LoadZipCodesAndGeos;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostalcodeService implements IPostalcodeService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PostalcodeService.class);

  private ImmutableMap<String, List<GeoPoint>> postalCodes;

  @Inject
  public PostalcodeService(@PropPostalcodesPath String postalCodePath) {
    this.postalCodes = ImmutableMap.of();
    try {
      loadPostalCodes(new File(postalCodePath));
    } catch (IOException e) {
      LOGGER.error("Failed to load postalcodes from '{}'", postalCodePath);
    }
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
    // TODO: Implement this
  }
}
