package com.github.ssullivan.tasks.feeds;

import com.amazonaws.services.s3.AmazonS3;
import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IServiceCodesDao;
import com.github.ssullivan.model.collections.Tuple2;
import com.github.ssullivan.model.datafeeds.SamshaLocatorData;
import com.github.ssullivan.utils.ShortUuid;
import com.google.common.io.Resources;
import io.dropwizard.testing.FixtureHelpers;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@TestInstance(Lifecycle.PER_CLASS)
public class SamshaLocatorEtlTest {

  private static MockWebServer mockWebServer = new MockWebServer();


  @BeforeAll
  public static void setup() throws IOException {
    mockWebServer.start(randomPort());
  }

  @AfterAll
  public static void teardown() throws IOException {
    mockWebServer.shutdown();
  }

  public static int randomPort() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      return serverSocket.getLocalPort();
    }
  }


  @Test
  public void testFetchingData() throws IOException, InterruptedException {
    AmazonS3 amazonS3 = Mockito.mock(AmazonS3.class);
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)

        .setBody(FixtureHelpers.fixture("fixtures/Robots.txt"))
    );
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/octet-stream")
        .setBody(Resources
            .toString(Resources.getResource("fixtures/Locator.xlsx"), Charset.defaultCharset()))
    );

    String s3Url = mockWebServer.url("").toString();

    FetchSamshaDataFeed fetchSamshaDataFeed = new FetchSamshaDataFeed(0L, s3Url, "test", amazonS3);
    Optional<Tuple2<String, String>> result = fetchSamshaDataFeed.get();

    final RecordedRequest robotsRequest = mockWebServer.takeRequest(30, TimeUnit.SECONDS);
    final RecordedRequest locatorRequest = mockWebServer.takeRequest(30, TimeUnit.SECONDS);

    MatcherAssert.assertThat(result.get().get_1(), Matchers.equalTo("test"));
    MatcherAssert.assertThat(result.get().get_2(), Matchers.startsWith("samsha/locations-"));
    MatcherAssert.assertThat(result.get().get_2(), Matchers.endsWith(".xlsx"));
  }

  @Test
  public void testFetchingFailed() throws IOException {
    AmazonS3 amazonS3 = Mockito.mock(AmazonS3.class);
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setBody(FixtureHelpers.fixture("fixtures/Robots.txt"))
    );
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(500)
        .setHeader("Content-Type", "application/html")
    );
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(500)
        .setHeader("Content-Type", "application/html")
    );
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(500)
        .setHeader("Content-Type", "application/html")
    );
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(500)
        .setHeader("Content-Type", "application/html")
    );
    String s3Url = mockWebServer.url("").toString();
    FetchSamshaDataFeed fetchSamshaDataFeed = new FetchSamshaDataFeed(0L, s3Url, "test", amazonS3);
    Optional<Tuple2<String, String>> result = fetchSamshaDataFeed.get();
    MatcherAssert.assertThat(result.isPresent(), Matchers.equalTo(false));
  }


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
        .setBody(FixtureHelpers.fixture("fixtures/Robots.txt"))
    );
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/octet-stream")
        .setBody(Resources
            .toString(Resources.getResource("fixtures/Locator.xlsx"), Charset.defaultCharset()))
    );

    FetchSamshaDataFeed fetchSamshaDataFeed = new FetchSamshaDataFeed(0L, "http://localhost:8181",
        "test", amazonS3);
    Optional<Tuple2<String, String>> result = fetchSamshaDataFeed.get();
    MatcherAssert.assertThat(result.isPresent(), Matchers.equalTo(false));
  }

  @Test
  public void testTransformLocatorSpreadheet() throws IOException {
    TransformLocatorSpreadsheet transformLocatorSpreadsheet = new TransformLocatorSpreadsheet();
    Optional<SamshaLocatorData> data = transformLocatorSpreadsheet
        .apply(ShortUuid.randomShortUuid(),
            Resources.getResource("fixtures/Locator.xlsx").openStream());

    MatcherAssert.assertThat(data.isPresent(), Matchers.equalTo(true));
    MatcherAssert.assertThat(data.get().isGood(), Matchers.equalTo(true));
    MatcherAssert.assertThat(data.get().getServices().size(), Matchers.equalTo(190));
    MatcherAssert.assertThat(data.get().getCategories().size(), Matchers.equalTo(27));
  }

  @Test
  public void testStoringData() {
    IFeedDao feedDao = Mockito.mock(IFeedDao.class);
    ICategoryCodesDao categoryCodesDao = Mockito.mock(ICategoryCodesDao.class);
    IServiceCodesDao serviceCodesDao = Mockito.mock(IServiceCodesDao.class);
    IFacilityDao facilityDao = Mockito.mock(IFacilityDao.class);

    StoreSamshaLocatorData storeSamshaLocatorData = new StoreSamshaLocatorData(feedDao,
        categoryCodesDao, serviceCodesDao,
        facilityDao
    );

    SamshaLocatorData samshaLocatorData = new SamshaLocatorData(ShortUuid.randomShortUuid(),
        new HashSet<>(),
        new HashSet<>(),
        new ArrayList<>()
    );

    MatcherAssert.assertThat(storeSamshaLocatorData.apply(samshaLocatorData),
        Matchers.equalTo(true));


  }


}
