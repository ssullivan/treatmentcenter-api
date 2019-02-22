package com.github.ssullivan.db.redis.index;

import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IndexFacility;
import com.github.ssullivan.db.IndexFacilityByCategoryCode;
import com.github.ssullivan.db.IndexFacilityByGeo;
import com.github.ssullivan.db.IndexFacilityByServiceCode;
import com.github.ssullivan.model.Facility;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RedisFacilityIndexDao implements IndexFacility {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisFacilityIndexDao.class);

  private IndexFacilityByGeo byGeo;
  private IndexFacilityByServiceCode byServiceCode;
  private IndexFacilityByCategoryCode byCategoryCode;
  private IFeedDao feedDao;

  @Inject
  public RedisFacilityIndexDao(IndexFacilityByGeo byGeo,
      IndexFacilityByServiceCode byServiceCode,
      IndexFacilityByCategoryCode byCategoryCode,
      IFeedDao feedDao) {
    this.byGeo = byGeo;
    this.byServiceCode = byServiceCode;
    this.byCategoryCode = byCategoryCode;
    this.feedDao = feedDao;
  }

  @Override
  public void index(String feed, Facility facility) throws IOException {
    byGeo.index(feed, facility);
    byServiceCode.index(feed, facility);
    byCategoryCode.index(feed, facility);
    LOGGER.debug("Index facility {} in feed {}", facility.getId(), facility.getFeedId());
  }

  @Override
  public void index(String feed, List<Facility> batch) throws IOException {
    byGeo.index(feed, batch);
    byServiceCode.index(feed, batch);
    byCategoryCode.index(feed, batch);
    LOGGER.debug("Index facility {} in feed {}", batch, feed);
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
  public void expire(String feed, long seconds, boolean overwrite) throws Exception {
    LOGGER.info("expire indices for feed {} after {} seconds", feed, seconds);
    this.byGeo.expire(feed, seconds, overwrite);
    this.byServiceCode.expire(feed, seconds, overwrite);
    this.byCategoryCode.expire(feed, seconds, overwrite);
  }

}
