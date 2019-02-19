package com.github.ssullivan.tasks.feeds;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.github.ssullivan.model.collections.Tuple2;
import com.github.ssullivan.model.datafeeds.SamshaLocatorData;
import com.google.common.base.Stopwatch;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SamshaLocatorEtl implements ISamshaEtlJob, IEtlJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(SamshaLocatorEtl.class);

  private FetchSamshaDataFeed fetchSamshaDataFeed;
  private TransformLocatorSpreadsheet transformLocatorSpreadsheet;
  private StoreSamshaLocatorData storeSamshaLocatorData;

  private Optional<String> locatorBucket = Optional.empty();
  private Optional<String> locatorObjectKey = Optional.empty();
  private Optional<SamshaLocatorData> samshaLocatorData = Optional.empty();
  private ManageFeeds manageFeeds;

  private String feedId;
  private AmazonS3 amazonS3;


  @Inject
  public SamshaLocatorEtl(final FetchSamshaDataFeed fetchSamshaDataFeed,
      final TransformLocatorSpreadsheet transformLocatorSpreadsheet,
      final StoreSamshaLocatorData storeSamshaLocatorData,
      final ManageFeeds manageFeeds,
      final AmazonS3 amazonS3) {

    this.fetchSamshaDataFeed = fetchSamshaDataFeed;
    this.transformLocatorSpreadsheet = transformLocatorSpreadsheet;
    this.storeSamshaLocatorData = storeSamshaLocatorData;
    this.amazonS3 = amazonS3;
    this.manageFeeds = manageFeeds;
  }

  @Override
  public void extract() throws IOException {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    LOGGER.info("[extract] started at {}", System.currentTimeMillis());
    try {
      Optional<Tuple2<String, String>> bucketAndObjectKey = fetchSamshaDataFeed.get();
      if (bucketAndObjectKey.isPresent()) {
        LOGGER.info("Successfully downloaded SAMSHA locator to bucket {}",
            bucketAndObjectKey.get().get_2());
        this.locatorBucket = Optional.of(bucketAndObjectKey.get().get_1());
        this.locatorObjectKey = Optional.of(bucketAndObjectKey.get().get_2());
      } else {
        LOGGER.error("Failed to donwnload SAMSHA locator to bucket!");
        throw new IOException("Failed to donwnload SAMSHA locator to bucket!");
      }
    } finally {
      stopwatch.stop();
      LOGGER
          .info("[extract] Finished after {} seconds ({} ms)", stopwatch.elapsed(TimeUnit.SECONDS),
              stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
  }

  @Override
  public void transform() throws IOException {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    LOGGER.info("[transform] started at {}", System.currentTimeMillis());
    try {
      if (!this.locatorObjectKey.isPresent() || !this.locatorBucket.isPresent()) {
        throw new IOException(
            "No data to transform [bucket or locator key was empty]! Did you run extract?");
      }

      final S3Object s3Object = amazonS3.getObject(locatorBucket.get(), locatorObjectKey.get());
      this.feedId = s3Object.getObjectMetadata()
          .getUserMetadata()
          .get("feed_id");

      LOGGER.info("Loading data for feed_id: {}", feedId);

      try (final S3ObjectInputStream inputStream = s3Object.getObjectContent();
          final BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)
      ) {
        samshaLocatorData = transformLocatorSpreadsheet.apply(this.feedId, bufferedInputStream);
      }
    } finally {
      stopwatch.stop();
      LOGGER.info("[transform] Finished after {} seconds ({} ms)",
          stopwatch.elapsed(TimeUnit.SECONDS), stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
  }

  @Override
  public void load() throws IOException {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      if (!this.samshaLocatorData.isPresent()) {
        throw new IOException(
            "No data to transform [no parsed locator data available]! Did you run extract, and transform?");
      }

      if (samshaLocatorData.get().isGood()) {
        if (!this.storeSamshaLocatorData.apply(samshaLocatorData.get())) {
          LOGGER.warn("Failed to store SAMSHA locator data!");
        } else {
          try {
            this.manageFeeds.expireOldFeeds(this.samshaLocatorData.get().getFeedId());
          } catch (Exception e) {
            LOGGER.error("Failed to expire old keys", e);
          }
        }
      } else {
        LOGGER.warn("The data feed {} @ {}/{} has bad/invalid data!", feedId,
            this.locatorBucket.get(), this.locatorObjectKey.get());
      }
    } finally {
      stopwatch.stop();
      LOGGER.info("[load] Finished after {} seconds ({} ms)", stopwatch.elapsed(TimeUnit.SECONDS),
          stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
  }
}
