package com.github.ssullivan.db.redis.search;

import static com.github.ssullivan.db.redis.RedisConstants.INDEX_BY_SERVICES;

import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.redis.IAsyncRedisConnectionPool;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFindFacility {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFindFacility.class);

  protected IRedisConnectionPool syncPool;
  protected IFacilityDao facilityDao;
  protected IFeedDao feedDao;
  protected IAsyncRedisConnectionPool asyncPool;

  public AbstractFindFacility(IRedisConnectionPool redis,
      IAsyncRedisConnectionPool asyncPool,
      IFacilityDao facilityDao,
      IFeedDao feedDao) {
    this.syncPool = redis;
    this.facilityDao = facilityDao;
    this.feedDao = feedDao;
    this.asyncPool = asyncPool;
  }


  protected static String[] getServiceCodeIndices(final Collection<String> serviceCodes) {
    return getServiceCodeIndices("", serviceCodes);
  }

  protected static String[] getServiceCodeIndices(final String feed,
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


}
