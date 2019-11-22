package com.github.ssullivan.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.PostgresTestUtils;
import com.github.ssullivan.db.IRecoveryHousingDao;
import com.github.ssullivan.guice.PsqlClientModule;
import com.github.ssullivan.guice.google.GoogleMockCredentialProvider;
import com.github.ssullivan.guice.google.GoogleMockTransportProvider;
import com.github.ssullivan.guice.google.GoogleSheetsClientProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.testing.auth.oauth2.MockGoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.services.sheets.v4.Sheets;
import com.google.inject.*;
import com.zaxxer.hikari.HikariDataSource;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GoogleSheetsReaderTest {
    private static final ObjectMapper Jackson = new ObjectMapper();

    @Inject
    private HikariDataSource hikariDataSource;

    @Inject
    private DSLContext dslContext;

    @Inject
    private IRecoveryHousingDao dao;

    @Inject
    private MockGoogleCredential googleCredential;

    @Inject
    private MockHttpTransport httpTransport;

    @Inject
    private GoogleSheetsReader googleSheetsReader;


    @BeforeAll
    public void setup() {
        Injector injector = Guice.createInjector(new PsqlClientModule(PostgresTestUtils.DbConfig),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Sheets.class).toProvider(GoogleSheetsClientProvider.class);

                        bind(GoogleMockCredentialProvider.class).in(Singleton.class);
                        bind(GoogleMockTransportProvider.class).in(Singleton.class);

                        bind(MockHttpTransport.class).toProvider(GoogleMockTransportProvider.class).in(Singleton.class);
                        bind(HttpTransport.class).toProvider(GoogleMockTransportProvider.class).in(Singleton.class);
                        bind(MockGoogleCredential.class).toProvider(GoogleMockCredentialProvider.class).in(Singleton.class);
                        bind(GoogleCredential.class).toProvider(GoogleMockCredentialProvider.class).in(Singleton.class);
                    }
                }
        );

        injector.injectMembers(this);

        PostgresTestUtils.setupSchema(dslContext, hikariDataSource);
    }

    @AfterAll
    public void teardown() {
        PostgresTestUtils.dropSchema(dslContext);
    }

    @Test
    public void testSheetsReaderConstruction() throws IOException {
        MatcherAssert.assertThat(googleSheetsReader, Matchers.notNullValue());
        // TODO: (1) Figure out what the JSON response for get on spreadsheets looks like
        // TODO: (2) Figure out what the JSON response for sheets looks like
        // TODO: (3) Figure out what the JSON response for ValueRange looks like
    }
}
