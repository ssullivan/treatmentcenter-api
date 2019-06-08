package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.PostgresTestUtils;
import com.github.ssullivan.db.IRecoveryHousingDao;
import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.db.psql.tables.records.RecoveryHousingRecord;
import com.github.ssullivan.guice.PsqlClientModule;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RecoveryHousingDaoTest {
    private static final ObjectMapper Jackson = new ObjectMapper();

    @Inject
    private HikariDataSource hikariDataSource;

    @Inject
    private DSLContext dslContext;

    @Inject
    private IRecoveryHousingDao dao;

    private  RecoveryHousingRecord recoveryHousingRecord = new RecoveryHousingRecord()
        .setCity("Test")
        .setState("Test")
        .setContactEmail("test@test.com")
        .setName("Test")
        .setContactName("Test")
        .setFeedName("Test")
        .setFeedRecordId(100L)
        .setFeedVersion(System.currentTimeMillis())
        .setPostalcode("21043")
        .setCapacity(100);

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

    @AfterEach
    public void truncate() {
      dslContext.truncate(Tables.RECOVERY_HOUSING);
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
        int result = dao.upsert(Collections.singletonList(recoveryHousingRecord));
        MatcherAssert.assertThat(result, Matchers.equalTo(1));
    }

    @Test
    public void testDeleteOldVersion() {
        Long firstFeedVersion = System.currentTimeMillis();
        List<RecoveryHousingRecord> firstBatch = testRecords(firstFeedVersion, 10);
        int firstResult = dao.upsert(firstBatch);
        MatcherAssert.assertThat(firstResult, Matchers.equalTo(firstBatch.size()));

        Long secondFeedVersion = System.currentTimeMillis();
        List<RecoveryHousingRecord> secondBatch = testRecords(secondFeedVersion, 10);
        MatcherAssert.assertThat(dao.upsert(secondBatch), Matchers.equalTo(secondBatch.size()));

        MatcherAssert.assertThat(dao.deleteByVersion("Test", Range.lessThan(secondFeedVersion)), Matchers.equalTo(10));
        MatcherAssert.assertThat(dao.count(ImmutableMap.of("feed_name", "Test")), Matchers.equalTo(10L));
    }

    private static List<RecoveryHousingRecord> testRecords(long feedVersion, int total) {
      List<RecoveryHousingRecord> batch = new ArrayList<>(total);
      for (int i = 0; i < total; ++i) {
        batch.add(new RecoveryHousingRecord()
            .setCity("Test" + i)
            .setState("Test" + i)
            .setContactEmail(i + "test@test.com")
            .setName("Test" + i)
            .setContactName("Test" + i)
            .setFeedName("Test")
            .setFeedRecordId(100L)
            .setFeedVersion(feedVersion)
            .setPostalcode("21043")
            .setCapacity(100));
      }
      return batch;
    }
}
