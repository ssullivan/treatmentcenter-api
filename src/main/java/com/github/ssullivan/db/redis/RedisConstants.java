package com.github.ssullivan.db.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.utils.ShortUuid;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class RedisConstants {

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

  public static final long DEFAULT_TIMEOUT = 1500;
  public static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

  public static final String SEARCH_REQ = "search:counter";
  public static final String SEARCH_BY_SERVICE_REQ = "search:services:counter";

  public static final String TREATMENT_FACILITIES = "facilities";
  public static final String TREATMENT_FACILITIES_IDS = TREATMENT_FACILITIES + ":feed:ids:";
  public static final String INDEX_BY_SERVICES = "index:service";
  public static final String INDEX_BY_CATEGORIES = "index:category";
  public static final String INDEX_BY_GEO = "index:geo";
  public static final int DEFAULT_EXPIRE_SECONDS = 120;


  public static String indexByGeoKey(final String feed) {
    String key = INDEX_BY_GEO;
    if (feed != null && !feed.isEmpty()) {
      key = key + ":" + feed;
    }
    return key;
  }

  public static String[] getServiceCodeIndices(final String feed,
      final Collection<String> serviceCodes) {
    if (null == serviceCodes || serviceCodes.isEmpty()) {
      return new String[]{};
    }
    return serviceCodes
        .stream()
        .map(code -> {
          if (feed == null || feed.isEmpty()) {
            return INDEX_BY_SERVICES + ":" + code;
          } else {
            return INDEX_BY_SERVICES + ":" + feed + ":" + code;
          }
        })
        .collect(Collectors.toSet()).toArray(new String[]{});
  }

  public static String[] getServiceCodeIndices(final Collection<String> serviceCodes) {
    return getServiceCodeIndices("", serviceCodes);
  }


  public static boolean isValidIdentifier(final String id) {
    return id != null && !id.isEmpty() && id.length() > 1 && ShortUuid.isValid(id);
  }


  public static boolean isEmpty(final Collection<String> list) {
    return list == null || list.isEmpty();
  }
}
