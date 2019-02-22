package com.github.ssullivan.db.redis.index;

import static com.github.ssullivan.db.redis.RedisConstants.INDEX_BY_CATEGORIES;
import static com.github.ssullivan.db.redis.RedisConstants.isValidIdentifier;

import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IndexFacilityByCategoryCode;
import com.github.ssullivan.db.redis.IRedisConnectionPool;
import com.github.ssullivan.model.Facility;
import io.lettuce.core.api.sync.RedisCommands;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RedisIndexFacilityByCategoryCode extends AbstractRedisIndexFacility implements
    IndexFacilityByCategoryCode {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(RedisIndexFacilityByServiceCode.class);

  @Inject
  public RedisIndexFacilityByCategoryCode(IRedisConnectionPool pool, IFeedDao feedDao) {
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

    facility.getCategoryCodes()
        .stream()
        .filter(Objects::nonNull)
        .filter(it -> !it.isEmpty())
        .forEach(code -> {
          String key = INDEX_BY_CATEGORIES + ":" + code;
          if (feed != null && !feed.isEmpty()) {
            key = INDEX_BY_CATEGORIES + ":" + feed + ":" + code;
          }
          Long totalResults = sync.sadd(key, facility.getId());

          LOGGER.debug("Added {} keys to {}", totalResults, key);
        });
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
  public void expire(String feed, long seconds, boolean overwrite) throws IOException {
    expireMatching(INDEX_BY_CATEGORIES + ":" + feed + ":", seconds, overwrite);
  }
}
