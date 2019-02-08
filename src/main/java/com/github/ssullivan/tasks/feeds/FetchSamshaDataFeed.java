package com.github.ssullivan.tasks.feeds;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.ssullivan.guice.BucketName;
import com.github.ssullivan.guice.SamshaUrl;
import com.github.ssullivan.model.collections.Tuple2;
import com.github.ssullivan.model.datafeeds.Feed;
import com.github.ssullivan.utils.ShortUuid;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches the raw XLSX from samsha and stores it in an S3 bucket like:
 *
 * General format is: {bucket}/feeds/{feedName}/{prefix}-{timestamp}.{type}
 * {bucket}/feeds/{feedname}.feed.json
 *
 * {bucket}/feeds/samsha/locations-{timestamp}.xlsx {bucket}/feeds/samsha.feed.json
 */
public class FetchSamshaDataFeed implements Supplier<Optional<Tuple2<String, String>>>,
    Function<Void, Optional<Tuple2<String, String>>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FetchSamshaDataFeed.class);
  private static final ObjectReader FEED_READER = new ObjectMapper().readerFor(Feed.class);
  private static final ObjectWriter FEED_WRITER = new ObjectMapper().writerFor(Feed.class);
  private static final String SAMSHA = "samsha";

  private String url;
  private String bucket;
  private AmazonS3 amazonS3;

  @Inject
  public FetchSamshaDataFeed(@SamshaUrl final String url, @BucketName final String bucket,
      final AmazonS3 amazonS3) {
    this.url = url;
    this.bucket = bucket;
    this.amazonS3 = amazonS3;
  }

  @Override
  public Optional<Tuple2<String, String>> get() {
    if (this.url.startsWith("file")) {
      final File file = new File(this.url.replaceFirst("file:/", ""));
      try (FileInputStream fileInputStream = new FileInputStream(file);
          BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream, 8192)
      ) {
        return Optional.of(handleStream(file.length(), bufferedInputStream));
      } catch (MalformedURLException e) {
        LOGGER.error("Failed to load: " + this.url, e);
      } catch (IOException e) {
        LOGGER.error("Failed to load: " + this.url, e);
      }
      return Optional.empty();
    } else {
      final Client client = JerseyClientBuilder.createClient();
      try {
        final Response response = client.target(url)
            .path("locatorExcel")
            .queryParam("sType", "SA")
            .request()
            .buildGet()
            .invoke();

        if (response.getStatus() == 200) {
          try (final InputStream inputStream = response.readEntity(InputStream.class);
              final BufferedInputStream bufferedInputStream = new BufferedInputStream(
                  inputStream)) {
            return Optional.of(handleStream(response.getLength(), bufferedInputStream));
          } catch (IOException e) {
            LOGGER.error("Failed to download the SAMSHA locatorExcel", e);
          }
        }
      } finally {
        client.close();
      }
    }

    return Optional.empty();
  }

  private Tuple2<String, String> handleStream(final long contentLength,
      final InputStream inputStream)
      throws IOException {
    LOGGER.info("Successfully, fetched SAMSHA treatment facilities excel");

    final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
    final ObjectMetadata objectMetadata = new ObjectMetadata();

    final long collected_at = System.currentTimeMillis();
    final String objectKey = createObjectKey();
    final long epochMillis = System.currentTimeMillis();
    final Feed feed = new Feed(SAMSHA, objectKey, epochMillis);

    objectMetadata.addUserMetadata("collected_at", "" + collected_at);
    objectMetadata.addUserMetadata("feed_id", ShortUuid.randomShortUuid());

    if (contentLength > 0) {
      objectMetadata.setContentLength(contentLength);
    }

    amazonS3.putObject(this.bucket, objectKey, inputStream, objectMetadata);

    try (final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
        FEED_WRITER.writeValueAsBytes(feed))) {
      amazonS3.putObject(this.bucket, "feeds/samsha.feed.json", byteArrayInputStream,
          new ObjectMetadata());
    }

    return new Tuple2<>(bucket, objectKey);
  }

  private String createObjectKey() {
    return "samsha/locations-" + ZonedDateTime.now(ZoneOffset.UTC)
        .format(DateTimeFormatter.ofPattern("YYYYMMddHHmmss")) + ".xlsx";
  }

  private Optional<Feed> getFeed() {
    final S3Object s3Object = amazonS3.getObject(this.bucket, "feeds/samsha.feed.json");

    try (S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(s3ObjectInputStream)) {
      return FEED_READER.readValue(bufferedInputStream);
    } catch (IOException e) {
      LOGGER.error("Failed to fetch samsha.feed.json", e);
    }

    return Optional.empty();
  }

  @Override
  public Optional<Tuple2<String, String>> apply(Void aVoid) {
    return get();
  }
}
