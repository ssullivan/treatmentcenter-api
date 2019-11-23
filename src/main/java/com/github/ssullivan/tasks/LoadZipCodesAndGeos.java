package com.github.ssullivan.tasks;

import com.github.ssullivan.model.GeoPoint;
import com.google.common.collect.ImmutableMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadZipCodesAndGeos {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoadZipCodesAndGeos.class);

  /**
   * country code      : iso country code, 2 characters //  postal code       : varchar(20) // place
   * name        : varchar(180) //  admin name1       : 1. order subdivision (state) varchar(100) //
   * admin code1       : 1. order subdivision (state) varchar(20) //  admin name2 : 2. order
   * subdivision (county/province) varchar(100) //  admin code2       : 2. order subdivision
   * (county/province) varchar(20) //  admin name3       : 3. order subdivision (community)
   * varchar(100) //  admin code3       : 3. order subdivision (community) varchar(20) //  latitude
   * : estimated latitude (wgs84) //  longitude         : estimated longitude (wgs84) // accuracy :
   * accuracy of lat/lng from 1=estimated to 6=centroid
   **/
  private static final String ISO_COUNTRY_CODE = "(?<isoCountryCode>\\w{2})";
  private static final String POSTAL_CODE = "(?<postalCode>[^\t]{0,20})";
  private static final String PLACE_NAME = "(?<placeName>[^\t]{0,180})";
  private static final String ADMIN_NAME_1 = "(?<adminName1>[^\t]{0,100})";
  private static final String ADMIN_CODE_1 = "(?<adminCode1>[^\t]{0,20})";
  private static final String ADMIN_NAME_2 = "(?<adminName2>[^\t]{0,100})";
  private static final String ADMIN_CODE_2 = "(?<adminCode2>[^\t]{0,20})";
  private static final String ADMIN_NAME_3 = "(?<adminName3>[^\t]{0,30})";
  private static final String ADMIN_CODE_3 = "(?<adminCode3>[^\t]{0,20})";
  private static final String LATITUDE = "(?<latitude>[+-]?\\d+(\\.\\d+)?)";
  private static final String LONGITUDE = "(?<longitude>[+-]?\\d+(\\.\\d+)?)";
  private static final String ACCURACY = "(?<accuracy>[1-6]{0,1})";

  private static final Pattern RECORD_RE = Pattern.compile(
      ISO_COUNTRY_CODE
          + "\t"
          + POSTAL_CODE
          + "\t"
          + PLACE_NAME
          + "\t"
          + ADMIN_NAME_1
          + "\t"
          + ADMIN_CODE_1
          + "\t"
          + ADMIN_NAME_2
          + "\t"
          + ADMIN_CODE_2
          + "\t"
          + ADMIN_NAME_3
          + "\t"
          + ADMIN_CODE_3
          + "\t"
          + LATITUDE
          + "\t"
          + LONGITUDE
          + "\t"
          + ACCURACY
  );


  public ImmutableMap<String, List<GeoPoint>> parse(final File file) throws IOException {
    try (FileInputStream fin = new FileInputStream(file)) {
      return parse(fin);
    }
  }

  public ImmutableMap<String, List<GeoPoint>> parse(final InputStream inputStream)
      throws IOException {

    final Map<String, List<GeoPoint>> geoPointMap = new HashMap<>();

    try (BufferedReader bufferedReader = new BufferedReader(
        new InputStreamReader(inputStream, "UTF-8"))) {
      String line = null;
      while ((line = bufferedReader.readLine()) != null) {
        if (line.isEmpty()) {
          continue;
        }

        final Matcher matcher = RECORD_RE.matcher(line);
        if (matcher.matches()) {
          final String postalCode = matcher.group("postalCode");
          final String latitude = matcher.group("latitude");
          final String longitude = matcher.group("longitude");

          List<GeoPoint> existingGeoPoints = geoPointMap.get(postalCode);
          final GeoPoint geoPoint = GeoPoint.geoPoint(Double.parseDouble(latitude),
              Double.parseDouble(longitude));

          if (existingGeoPoints == null) {
            existingGeoPoints = new ArrayList<>();
          }

          existingGeoPoints.add(geoPoint);
          geoPointMap.put(postalCode, existingGeoPoints);
        }
      }
    }

    return ImmutableMap.copyOf(geoPointMap);
  }
}
