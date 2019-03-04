package com.github.ssullivan.tasks.feeds;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.ssullivan.core.RobotsTxtParser;
import com.github.ssullivan.guice.BucketName;
import com.github.ssullivan.guice.CrawlDelay;
import com.github.ssullivan.guice.SamshaUrl;
import com.github.ssullivan.model.collections.Tuple2;
import com.github.ssullivan.model.crawler.RobotsTxt;
import com.github.ssullivan.model.datafeeds.Feed;
import com.github.ssullivan.utils.ShortUuid;
import com.google.common.io.ByteStreams;
import com.google.common.primitives.Bytes;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
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
public class FetchSamshaDataFeed implements ILoadSamshaSpreadsheet, Supplier<Optional<Tuple2<String, String>>>,
    Function<Void, Optional<Tuple2<String, String>>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FetchSamshaDataFeed.class);
  private static final ObjectReader FEED_READER = new ObjectMapper().readerFor(Feed.class);
  private static final ObjectWriter FEED_WRITER = new ObjectMapper().writerFor(Feed.class);
  private static final String SAMSHA = "samsha";
  private static final int DefaultBufferSize = 8192 * 4;
  private static final String UserAgent = "Mozilla/5.0 (compatible; Safebot/1.13.0; https://www.safeproject.us/)";
  private String url;
  private String bucket;
  private AmazonS3 amazonS3;
  private Client client;
  private long crawlDelay;
  private boolean cacheInMemory;
  private byte[] cachedBytes;


  @Inject
  public FetchSamshaDataFeed(@CrawlDelay final Long delay, @SamshaUrl final String url,
      @BucketName final String bucket,
      final AmazonS3 amazonS3) {
    this.url = url;
    this.bucket = bucket;
    this.amazonS3 = amazonS3;
    this.crawlDelay = delay;
    this.client = JerseyClientBuilder.createClient()
        .register(LoggingFeature.class)
        .register(MultiPartFeature.class)
        .register(GZipEncoder.class)
        .register(DeflateEncoder.class)
        .register((ClientRequestFilter) clientRequestContext -> {
          clientRequestContext.getHeaders().add(HttpHeaders.ACCEPT_ENCODING, "gzip");
          clientRequestContext.getHeaders().add(HttpHeaders.USER_AGENT, UserAgent);
          clientRequestContext.getHeaders().add(HttpHeaders.ACCEPT,
              "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
          clientRequestContext.getHeaders().add(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5");
          clientRequestContext.getHeaders().add("Upgrade-Insecure-Requests", "1");

        });
    this.cacheInMemory = false;
    this.cachedBytes = new byte[]{};
  }

  public InputStream newInputStream() {
    return new ByteArrayInputStream(cachedBytes);
  }

  public void setCacheInMemory(boolean cacheInMemory) {
    this.cacheInMemory = cacheInMemory;
  }

  public Optional<RobotsTxt> fetchRobotsTxt() {
    final WebTarget webTarget = client.target(url).path("robots.txt");
    try {
      LOGGER.info("Attempting to download robots.txt from {}", webTarget.getUri().toString());
      Response response = webTarget.request().buildGet().invoke();
      if (response.getStatus() == 200) {
        try (InputStream inputStream = response.readEntity(InputStream.class)) {
          RobotsTxtParser parser = new RobotsTxtParser();
          return Optional.ofNullable(parser.parse(inputStream));
        }
      } else {
        LOGGER.error("Failed to get robots.txt! Got HTTP {}", response.getStatus());
        return Optional.empty();
      }
    } catch (IOException e) {
      LOGGER.error("Failed to get robotx.txt", e);
    }
    return Optional.empty();
  }

  private Optional<Tuple2<String, String>> loadFile() {
    final File file = new File(this.url.replaceFirst("file:", ""));
    try (FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream,
            DefaultBufferSize)
    ) {
      if (this.cacheInMemory) {
        final ByteArrayOutputStream inMemBuffer = new ByteArrayOutputStream(8192 * 3);
        ByteStreams.copy(bufferedInputStream, inMemBuffer);
        this.cachedBytes = inMemBuffer.toByteArray();
        return Optional.of(new Tuple2<>(bucket,  createObjectKey()));
      }
      else {
        return Optional.of(handleStream(file.length(), bufferedInputStream));
      }
    } catch (IOException e) {
      LOGGER.error("Failed to load: " + this.url, e);
    }
    return Optional.empty();
  }

  private Optional<Tuple2<String, String>> loadURL() {
    Optional<RobotsTxt> robotsTxtOptional = fetchRobotsTxt();
    if (robotsTxtOptional.isPresent()) {
      LOGGER.info("Success! Fetched robots.txt file");
    }
    try {

      if (this.crawlDelay > 0) {
        Thread.sleep(this.crawlDelay);
      }

      final WebTarget locatorExcelTarget = client.target(url)
          .path("locatorExcel");
      int attempts = 1;
      try {
        do {
          LOGGER.info("Attempting to download spreadsheet from : {}", locatorExcelTarget);
          final FormDataMultiPart formDataMultiPart = new FormDataMultiPart();
          formDataMultiPart.field("sType", "SA");
          formDataMultiPart.field("page", "1");
          formDataMultiPart.field("includeServices", "Y");
          formDataMultiPart.field("sortIndex", "0");

          final Response response = locatorExcelTarget.request()
              .header("User-Agent", "")
              .header("TE", "Trailers")
              .buildPost(Entity.entity(formDataMultiPart, formDataMultiPart.getMediaType()))
              .invoke();

          if (response.getStatus() == 200) {
            LOGGER.info("Success! Downloading locator spreadsheet [{} bytes]",
                response.getLength());
            try (final InputStream inputStream = response.readEntity(InputStream.class);
                final BufferedInputStream bufferedInputStream = new BufferedInputStream(
                    inputStream, DefaultBufferSize)) {
              return Optional
                  .of(handleStream(response.getLength(), bufferedInputStream));

            } catch (IOException e) {
              LOGGER.error("Failed to download the SAMSHA locatorExcel", e);
            }
          } else {
            LOGGER.error("Received HTTP {}: Body: {}", response.getStatus(),
                response.readEntity(String.class));
          }

          if (this.crawlDelay > 0) {
            Thread.sleep(this.crawlDelay);
          }
        } while (attempts++ < 3);
      } catch (InterruptedException e) {
        LOGGER.error("Interrupted while attempting to download spreadsheet", e);
        Thread.currentThread().interrupt();
      }


    } catch (InterruptedException e) {
      LOGGER.error("Interrupted while attempting to download spreadsheet", e);
      Thread.currentThread().interrupt();
    } finally {
      client.close();
    }
    return Optional.empty();
  }

  @Override
  public Optional<Tuple2<String, String>> get() {
    if (this.url.startsWith("file")) {
      return loadFile();
    } else {
      return loadURL();
    }
  }

  private Tuple2<String, String> handleStream(final long contentLength,
      final InputStream inputStream)
      throws IOException {
    LOGGER.info("Successfully, fetched SAMSHA treatment facilities excel");

    final String objectKey = createObjectKey();
    final long epochMillis = System.currentTimeMillis();
    final Feed feed = new Feed(SAMSHA, objectKey, epochMillis);

    final ObjectMetadata objectMetadata = new ObjectMetadata();
    objectMetadata.addUserMetadata("collected_at", "" + System.currentTimeMillis());
    objectMetadata.addUserMetadata("feed_id", ShortUuid.randomShortUuid());

    if (contentLength > 0) {
      objectMetadata.setContentLength(contentLength);
    }

    final PutObjectRequest putObjectRequest = new PutObjectRequest(this.bucket, objectKey,
        inputStream, objectMetadata);
    putObjectRequest.getRequestClientOptions().setReadLimit(8192);

    PutObjectResult putObjectResult = amazonS3.putObject(putObjectRequest);

    if (putObjectResult != null) {
      LOGGER.info("[aws] Stored object {}/{} version {}", this.bucket, objectKey,
          putObjectResult.getVersionId());
    }

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
