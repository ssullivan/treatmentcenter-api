package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.PostgresTestUtils;
import com.github.ssullivan.db.IWorksheetDao;
import com.github.ssullivan.guice.PsqlClientModule;
import com.github.ssullivan.model.sheets.SheetRow;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.zaxxer.hikari.HikariDataSource;
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
public class WorksheetDaoTest {
    private static final ObjectMapper Jackson = new ObjectMapper();

    @Inject
    private HikariDataSource hikariDataSource;

    @Inject
    private DSLContext dslContext;

    @Inject
    private IWorksheetDao worksheetDao;


    @BeforeAll
    public void setup() {
        Injector injector = Guice.createInjector(new PsqlClientModule(PostgresTestUtils.DbConfig),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(IWorksheetDao.class).to(WorksheetDao.class);
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
    public void testNullSpreadsheetId() {
        try {
            worksheetDao.upsertBatch(null, null, null);
        }
        catch (NullPointerException e) {
            MatcherAssert.assertThat(e.getMessage(), Matchers.equalTo("spreadsheetId must not be null"));
        }
    }

    @Test
    public void testNullSheet() {
        try {
            worksheetDao.upsertBatch("Test", null, null);
        }
        catch (NullPointerException e) {
            MatcherAssert.assertThat(e.getMessage(), Matchers.equalTo("sheet must not be null"));
        }
    }

    @Test
    public void tesNullSheetId() {
        try {
            worksheetDao.upsertBatch( "test", new Sheet().setProperties(new SheetProperties()), null);
        }
        catch (NullPointerException e) {
            MatcherAssert.assertThat(e.getMessage(), Matchers.equalTo("sheetId must not be null"));
        }
    }

    @Test
    public void testNullRows() {
        try {
            worksheetDao.upsertBatch( "test", new Sheet().setProperties(new SheetProperties()
            .setIndex(0).setSheetId(0)), null);
        }
        catch (NullPointerException e) {
            MatcherAssert.assertThat(e.getMessage(), Matchers.equalTo("rows must not be null"));
        }
    }

    @Test
    public void testUpsert() {

        String spreadsheetId = "test";
        Sheet sheet = new Sheet()
                .setProperties(new SheetProperties()
                .setIndex(0)
                .setSheetId(100)
                .setTitle("Test"));

        List<SheetRow> sheetRows = generateSheetRows(10);
        int changed = worksheetDao.upsertBatch(spreadsheetId, sheet, sheetRows);

        MatcherAssert.assertThat(changed, Matchers.equalTo(sheetRows.size()));

        List<Map<String, Object>> expected = sheetRows.stream().map(SheetRow::getCells).collect(Collectors.toList());
        List<Map<String, Object>> fromDatabase = worksheetDao.listAll(spreadsheetId, sheet.getProperties().getSheetId())
                .stream().map(WorksheetDaoTest::toSheetRow)
                .collect(Collectors.toList());

        MatcherAssert.assertThat(fromDatabase, Matchers.containsInAnyOrder(expected.stream().map(WorksheetDaoTest::toMatcher).collect(Collectors.toList())));

        int j = 0;
    }

    static Matcher<Map<String, Object>> toMatcher(Map<String, Object> map) {
        return Matchers.allOf(map.entrySet().stream().map(entry -> Matchers.hasEntry(entry.getKey(), entry.getValue())).collect(Collectors.toList()));
    }

    static Map<String, Object> toSheetRow(JsonNode jsonNode) {
        return Jackson.convertValue(jsonNode, new TypeReference<LinkedHashMap<String, Object>>(){});
    }

    static List<SheetRow> generateSheetRows(int n) {
        List<SheetRow> toReturn = new ArrayList<>(n);
        for (int i = 1; i <= n; ++i) {
            toReturn.add(new SheetRow(cells(i), i));
        }
        return toReturn;
    }

    static LinkedHashMap cells(int n) {
        LinkedHashMap<String, Object> toReturn = new LinkedHashMap<>();
        toReturn.put("address", (123 + n) + " Test Street");
        toReturn.put("name", "Test" + n);
        toReturn.put("zipcode", 2000 + n);
        toReturn.put("state", "NY");
        return toReturn;
    }
}
