package com.github.ssullivan.tasks;

import com.github.ssullivan.model.GeoPoint;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.List;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class LoadZipCodesAndGeosTest {

  @Test
  public void testParse() throws IOException {
    LoadZipCodesAndGeos loadZipCodesAndGeos = new LoadZipCodesAndGeos();

    ImmutableMap<String, List<GeoPoint>> results = loadZipCodesAndGeos
        .parse(Resources.getResource("fixtures/ZipCodes.txt").openStream());

    MatcherAssert.assertThat(results, Matchers.notNullValue());
    MatcherAssert.assertThat(results.size(), Matchers.equalTo(3));

    MatcherAssert.assertThat(results.get("99583"),
        Matchers.containsInAnyOrder(GeoPoint.geoPoint(54.8542, -163.4113)));
  }
}
