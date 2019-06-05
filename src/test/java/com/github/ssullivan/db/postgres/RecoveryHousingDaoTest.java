package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.PostgresTestUtils;
import com.github.ssullivan.db.IRecoveryHousingDao;
import com.github.ssullivan.db.IWorksheetDao;
import com.github.ssullivan.db.psql.tables.records.RecoveryHousingRecord;
import com.github.ssullivan.guice.PsqlClientModule;
import com.github.ssullivan.model.sheets.SheetRow;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Collections;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecoveryHousingDaoTest {
    private static final ObjectMapper Jackson = new ObjectMapper();

    @Inject
    private HikariDataSource hikariDataSource;

    @Inject
    private DSLContext dslContext;

    @Inject
    private IRecoveryHousingDao dao;


    @BeforeAll
    public void setup() {
        Injector injector = Guice.createInjector(new PsqlClientModule(PostgresTestUtils.DbConfig),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(IRecoveryHousingDao.class).to(RecoveryHousingDao.class);
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
    public void testNullBatch() {
        try {
            dao.upsert(null);
        }
        catch (NullPointerException e) {
            MatcherAssert.assertThat(e.getMessage(), Matchers.equalTo("batch must not be null"));
        }
    }


    @Test
    public void testUpsert() {

        RecoveryHousingRecord recoveryHousingRecord = new RecoveryHousingRecord()
            .setCity("Test")
            .setState("Test")
            .setContactEmail("test@test.com")
            .setName("Test")
            .setContactName("Test")
            .setFeedName("Test")
            .setFeedRecordId(100L)
            .setPostalcode("21043")
            .setCapacity(100);

        int result = dao.upsert(Collections.singletonList(recoveryHousingRecord));
        MatcherAssert.assertThat(result, Matchers.equalTo(1));
    }

}
