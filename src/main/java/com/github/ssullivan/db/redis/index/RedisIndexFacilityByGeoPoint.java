package com.github.ssullivan.db.redis.index;

import static com.github.ssullivan.db.redis.RedisConstants.indexByGeoKey;
import static com.github.ssullivan.db.redis.RedisConstants.isValidIdentifier;

import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IndexFacilityByGeo;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.model.Facility;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RedisIndexFacilityByGeoPoint extends AbstractRedisIndexFacility implements
    IndexFacilityByGeo {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(RedisIndexFacilityByServiceCode.class);


  @Inject
  public RedisIndexFacilityByGeoPoint(IRedisConnectionPool pool, IFeedDao feedDao) {
    super(pool, feedDao);
  }

  @Override
  protected void index(final RedisCommands<String, String> sync, final String feed,
      final Facility facility) {
    if (facility == null) {
      return;
    }

    if (!isValidIdentifier(facility.getId())) {
      throw new IllegalArgumentException("id must be non zero");
    }

    if (facility.getLocation() == null) {
      LOGGER.warn("Facility {} does not have a location", facility.getId());
      return;
    }

    final Long geoAddCount = sync
        .geoadd(indexByGeoKey(feed), facility.getLocation().lon(), facility.getLocation().lat(),
            facility.getId());

    LOGGER.debug("Added {} keys to {}", geoAddCount, indexByGeoKey(feed));
  }

  @Override
  public void index(Facility facility) throws IOException {
    Optional<String> currentFeedId = feedDao.currentFeedId();
    if (!currentFeedId.isPresent()) {
      LOGGER.error("No current feed id!");
      throw new IOException("No current feed id is set");
    }
    index(feedDao.currentFeedId().get(), facility);
  }

  @Override
  public void expire(String feed, long seconds, final boolean overwrite) throws Exception {
    try (final StatefulRedisConnection<String, String> connection = this.pool.borrowConnection()) {
      RedisCommands<String, String> sync = connection.sync();
      final Long ttl = sync.ttl(indexByGeoKey(feed));
      if (!overwrite && ttl == null || ttl < 0) {
        if (connection.sync().expire(indexByGeoKey(feed), seconds)) {
          LOGGER.debug("expire {}, {}", indexByGeoKey(feed), seconds);
        }
      }
      else {
        if (connection.sync().expire(indexByGeoKey(feed), seconds)) {
          LOGGER.debug("expire {}, {}", indexByGeoKey(feed), seconds);
        }
      }
    }
  }

}
