package com.github.ssullivan.tasks.feeds;

import com.amazonaws.services.s3.AmazonS3;
import com.github.ssullivan.model.collections.Tuple2;
import com.github.ssullivan.tasks.feeds.FetchSamshaDataFeed;
import com.github.ssullivan.tasks.feeds.SamshaLocatorEtl;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.jetty.util.IO;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SamshaLocatorEtlTest {

  private static MockWebServer mockWebServer = new MockWebServer();

  @BeforeAll
  public static void setup() throws IOException {
    mockWebServer.start(8181);
  }

  @AfterAll
  public static void teardown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  public void testFetchingData() throws IOException {
    AmazonS3 amazonS3 = Mockito.mock(AmazonS3.class);
    mockWebServer.enqueue(new MockResponse()
      .setResponseCode(200)
        .setHeader("Content-Type", "application/octet-stream")
        .setBody(Resources.toString(Resources.getResource("fixtures/Locator.xlsx"), Charset.defaultCharset()))
    );

    FetchSamshaDataFeed fetchSamshaDataFeed = new FetchSamshaDataFeed("http://localhost:8181", "test", amazonS3);
    Optional<Tuple2<String, String>> result = fetchSamshaDataFeed.get();

    MatcherAssert.assertThat(result.get().get_1(), Matchers.equalTo("test"));
    MatcherAssert.assertThat(result.get().get_2(), Matchers.startsWith("samsha/locations-"));
    MatcherAssert.assertThat(result.get().get_2(), Matchers.endsWith(".xlsx"));
  }

  @Test
  public void testFetchingFailed() throws IOException {
    AmazonS3 amazonS3 = Mockito.mock(AmazonS3.class);
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(500)
        .setHeader("Content-Type", "application/html")
    );

    FetchSamshaDataFeed fetchSamshaDataFeed = new FetchSamshaDataFeed("http://localhost:8181", "test", amazonS3);
    Optional<Tuple2<String, String>> result = fetchSamshaDataFeed.get();
    MatcherAssert.assertThat(result.isPresent(), Matchers.equalTo(false));
  }

  @Test
  public void testStoringS3Failed() throws IOException {
    AmazonS3 amazonS3 = Mockito.mock(AmazonS3.class);
    Mockito.when(amazonS3.putObject(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenAnswer(new Answer<Object>() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            throw new IOException("");
          }
        });

    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/octet-stream")
        .setBody(Resources.toString(Resources.getResource("fixtures/Locator.xlsx"), Charset.defaultCharset()))
    );

    FetchSamshaDataFeed fetchSamshaDataFeed = new FetchSamshaDataFeed("http://localhost:8181", "test", amazonS3);
    Optional<Tuple2<String, String>> result = fetchSamshaDataFeed.get();
    MatcherAssert.assertThat(result.isPresent(), Matchers.equalTo(false));
  }
}
