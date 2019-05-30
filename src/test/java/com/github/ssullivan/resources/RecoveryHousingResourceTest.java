package com.github.ssullivan.resources;


import com.github.ssullivan.auth.ApiKeyContainerRequestFilter;
import com.github.ssullivan.core.IRecoveryHousingController;
import com.github.ssullivan.db.IApiKeyDao;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.Service;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.github.ssullivan.resources.RecoveryHousingResource.SPREADSHEET_ID;

@ExtendWith(DropwizardExtensionsSupport.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class RecoveryHousingResourceTest {
    private static final GenericType<LinkedHashMap<String, String>> MAP_TYPE = new GenericType<LinkedHashMap<String, String>>() {
    };
    private static final GenericType<List<Service>> LIST_SERVICES
            = new GenericType<List<Service>>() {
    };

    private static final IRecoveryHousingController mockController = Mockito.mock(IRecoveryHousingController.class);
    private static final IApiKeyDao mockApiKeyDao = Mockito.mock(IApiKeyDao.class);

    public static final ResourceExtension resources = ResourceExtension.builder()
            .addResource(new RecoveryHousingResource(mockController))
            .addProvider(new ApiKeyContainerRequestFilter(mockApiKeyDao))
            .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
            .build();

    @BeforeEach
    public void setup() {

    }

    @AfterEach
    public void teardown() {
        Mockito.reset(mockController);
        Mockito.reset(mockApiKeyDao);
    }

    @Test
    public void testInvalidApiRequest() {
        Mockito.when(mockApiKeyDao.isValidApiKey(Mockito.eq("Test")))
                .thenReturn(false);

        Response response = resources
                .target("recovery")
                .path("sync")
                .request()
                .header(ApiKeyContainerRequestFilter.ApiKeyHeader, "Test")
                .post(Entity.json(new LinkedHashMap<String, String>()));
        Map<String, String> entity = response.readEntity(MAP_TYPE);

        MatcherAssert.assertThat(response.getStatus(), Matchers.equalTo(403));
        MatcherAssert.assertThat(entity, Matchers.hasEntry("message", "A valid API-Key was not provided"));
    }

    @Test
    public void testEmptyValidApiRequest() {
        Mockito.when(mockApiKeyDao.isValidApiKey(Mockito.eq("Test")))
                .thenReturn(true);

        Response response = resources
                .target("recovery")
                .path("sync")
                .request()
                .header(ApiKeyContainerRequestFilter.ApiKeyHeader, "Test")
                .post(Entity.json(new LinkedHashMap<String, String>()));
        Map<String, String> entity = response.readEntity(MAP_TYPE);

        MatcherAssert.assertThat(response.getStatus(), Matchers.equalTo(400));
        MatcherAssert.assertThat(entity, Matchers.hasEntry("message", "Invalid sync request. Please specify a spreadsheetId, and publish status of {true|false}"));
    }

    @Test
    public void testMissingSpreadsheetId() {
        Mockito.when(mockApiKeyDao.isValidApiKey(Mockito.eq("Test")))
                .thenReturn(true);

        LinkedHashMap<String, String> request  = new LinkedHashMap<>();
        request.put(SPREADSHEET_ID, null);
        request.put(RecoveryHousingResource.PUBLISH, Boolean.toString(false));

        Response response = resources
                .target("recovery")
                .path("sync")
                .request()
                .header(ApiKeyContainerRequestFilter.ApiKeyHeader, "Test")
                .post(Entity.json(request));
        Map<String, String> entity = response.readEntity(MAP_TYPE);

        MatcherAssert.assertThat(response.getStatus(), Matchers.equalTo(400));
        MatcherAssert.assertThat(entity, Matchers.hasEntry("message", "Invalid sync request. Please specify a " + SPREADSHEET_ID));
    }

    @Test
    public void testValidSyncRequest() {
        Mockito.when(mockApiKeyDao.isValidApiKey(Mockito.eq("Test")))
                .thenReturn(true);
        LinkedHashMap<String, String> request  = new LinkedHashMap<>();
        request.put(SPREADSHEET_ID, "Test");
        request.put(RecoveryHousingResource.PUBLISH, Boolean.toString(false));

        Response response = resources
                .target("recovery")
                .path("sync")
                .request()
                .header(ApiKeyContainerRequestFilter.ApiKeyHeader, "Test")
                .post(Entity.json(request));
        Map<String, String> entity = response.readEntity(MAP_TYPE);

        MatcherAssert.assertThat(response.getStatus(), Matchers.equalTo(204));
    }

    @Test
    public void testInvalidMinOffset() {
        Mockito.when(mockApiKeyDao.isValidApiKey(Mockito.eq("Test")))
                .thenReturn(true);

        Response response = resources
                .target("recovery")
                .path("search")
                .queryParam("offset", Integer.MIN_VALUE)
                .request()
                .header(ApiKeyContainerRequestFilter.ApiKeyHeader, "Test")
                .get();

        MatcherAssert.assertThat(response.getStatus(), Matchers.equalTo(400));
    }

    @Test
    public void testInvalidMaxOffset() {
        Mockito.when(mockApiKeyDao.isValidApiKey(Mockito.eq("Test")))
                .thenReturn(true);

        Response response = resources
                .target("recovery")
                .path("search")
                .queryParam("offset", Integer.MAX_VALUE)
                .request()
                .header(ApiKeyContainerRequestFilter.ApiKeyHeader, "Test")
                .get();

        MatcherAssert.assertThat(response.getStatus(), Matchers.equalTo(400));
    }

    @Test
    public void testInvalidMinSize() {
        Mockito.when(mockApiKeyDao.isValidApiKey(Mockito.eq("Test")))
                .thenReturn(true);

        Response response = resources
                .target("recovery")
                .path("search")
                .queryParam("size", Integer.MIN_VALUE)
                .request()
                .header(ApiKeyContainerRequestFilter.ApiKeyHeader, "Test")
                .get();

        MatcherAssert.assertThat(response.getStatus(), Matchers.equalTo(400));
    }

    @Test
    public void testInvalidMaxSize() {
        Mockito.when(mockApiKeyDao.isValidApiKey(Mockito.eq("Test")))
                .thenReturn(true);

        Response response = resources
                .target("recovery")
                .path("search")
                .queryParam("size", Integer.MAX_VALUE)
                .request()
                .header(ApiKeyContainerRequestFilter.ApiKeyHeader, "Test")
                .get();

        MatcherAssert.assertThat(response.getStatus(), Matchers.equalTo(400));
    }

    @Test
    public void testValidOffsetAndSize() throws IOException {
        Mockito.when(mockApiKeyDao.isValidApiKey(Mockito.eq("Test")))
                .thenReturn(true);
        Mockito.when(mockController.listAll(Mockito.anyMap(), Mockito.eq(Page.page(0, 100))))
                .thenReturn(SearchResults.empty());

        Response response = resources
                .target("recovery")
                .path("search")
                .queryParam("offset", 0)
                .queryParam("size", 100)
                .request()
                .get();

        MatcherAssert.assertThat(response.getStatus(), Matchers.equalTo(200));
    }

    @Test
    public void testSearchFailed() throws IOException {
        Mockito.when(mockApiKeyDao.isValidApiKey(Mockito.eq("Test")))
                .thenReturn(true);
        Mockito.when(mockController.listAll(Mockito.anyMap(), Mockito.eq(Page.page(0, 100))))
                .thenThrow(new IOException("Database error"));


        Response response = resources
                .target("recovery")
                .path("search")
                .queryParam("offset", 0)
                .queryParam("size", 100)
                .request()
                .get();



        MatcherAssert.assertThat(response.getStatus(), Matchers.equalTo(500));
    }
}
