package com.github.ssullivan.tasks.feeds;

import com.amazonaws.services.s3.AmazonS3;
import com.github.ssullivan.db.IManageFeeds;
import com.github.ssullivan.db.redis.RedisFeedManager;
import com.github.ssullivan.model.datafeeds.SamshaLocatorData;
import com.github.ssullivan.utils.ShortUuid;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemorySamshaLocalEtl implements ISamshaEtlJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(SamshaLocatorEtl.class);

  private FetchSamshaDataFeed fetchSamshaDataFeed;
  private TransformLocatorSpreadsheet transformLocatorSpreadsheet;
  private StoreSamshaLocatorData storeSamshaLocatorData;

  private Optional<String> locatorBucket = Optional.empty();
  private Optional<String> locatorObjectKey = Optional.empty();
  private Optional<SamshaLocatorData> samshaLocatorData = Optional.empty();
  private IManageFeeds manageFeeds;

  private String feedId;
  private AmazonS3 amazonS3;


  @Inject
  public InMemorySamshaLocalEtl(FetchSamshaDataFeed fetchSamshaDataFeed,
      final TransformLocatorSpreadsheet transformLocatorSpreadsheet,
      final StoreSamshaLocatorData storeSamshaLocatorData,
                                IManageFeeds manageFeeds) {
    this.fetchSamshaDataFeed = fetchSamshaDataFeed;
    this.transformLocatorSpreadsheet = transformLocatorSpreadsheet;
    this.storeSamshaLocatorData = storeSamshaLocatorData;
    this.manageFeeds = manageFeeds;
  }

  @Override
  public void extract() throws IOException {
    this.fetchSamshaDataFeed.setCacheInMemory(true);
    this.fetchSamshaDataFeed.get();
  }

  @Override
  public void transform() throws IOException {
    samshaLocatorData = this.transformLocatorSpreadsheet.apply(ShortUuid.randomShortUuid(), this.fetchSamshaDataFeed.newInputStream());
  }

  @Override
  public void load() throws IOException {
    samshaLocatorData.ifPresent(data -> {
      if (this.storeSamshaLocatorData.apply(data)) {
        try {
          this.manageFeeds.expireOldFeeds(data.getFeedId());
        } catch (Exception e) {
          LOGGER.error("Failed to expire old feeds", e);
        }
      }
    });
  }
}
