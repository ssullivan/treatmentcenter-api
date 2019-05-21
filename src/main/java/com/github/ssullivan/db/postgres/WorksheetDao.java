package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.db.IWorksheetDao;
import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.collections.Tuple2;
import com.google.common.collect.Lists;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class WorksheetDao implements IWorksheetDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorksheetDao.class);

    private final DSLContext dsl;
    private ObjectMapper objectMapper;

    @Inject
    public WorksheetDao(final DSLContext dslContext, final ObjectMapper objectMapper) {
        this.dsl = dslContext;
        this.objectMapper = objectMapper;
    }

    @Override
    public long getLatestVersion(final String spreadsheetId, final int worksheetId) {
        return dsl.select(DSL.max(Tables.WORKSHEET.VERSION))
                .where(Tables.WORKSHEET.SPREADSHEET_ID.eq(spreadsheetId)
                        .and(Tables.WORKSHEET.ID.eq(worksheetId)))
                .limit(1)
                .fetchOne()
                .value1();
    }

    @Override
    public List<JsonNode> listWorksheets(Page page) {
        return dsl.selectDistinct(Tables.WORKSHEET.SPREADSHEET_ID, Tables.WORKSHEET.ID, Tables.WORKSHEET.NAME)
                .orderBy(Tables.WORKSHEET.ID)
                .offset(page.offset())
                .limit(page.size())
                .fetch()
                .map(record -> objectMapper.createObjectNode()
                        .put(Tables.WORKSHEET.SPREADSHEET_ID.getName(), record.value1())
                        .put(Tables.WORKSHEET.ID.getName(), record.value2())
                        .put(Tables.WORKSHEET.NAME.getName(), record.value3()));
    }

    @Override
    public void deleteOldVersions() {

    }

    @Override
    public List<String> getColumnHeaders(String spreadsheetId, int worksheetId) {
        return dsl.transactionResult((TransactionalCallable<List<String>>) configuration -> {
            final DSLContext innerDsl = DSL.using(configuration);

            long maxVersion = innerDsl.select(DSL.max(Tables.WORKSHEET.ID))
                    .from(Tables.WORKSHEET)
                    .where(Tables.WORKSHEET.SPREADSHEET_ID.eq(spreadsheetId)
                            .and(Tables.WORKSHEET.ID.eq(worksheetId)))
                    .limit(1)
                    .fetchOne().value1();

           final String[] headers = innerDsl.select(Tables.WORKSHEET.COLUMN_HEADERS)
                    .from(Tables.WORKSHEET)
                    .where(Tables.WORKSHEET.VERSION.eq(maxVersion)
                        .and(Tables.WORKSHEET.SPREADSHEET_ID.eq(spreadsheetId)
                                .and(Tables.WORKSHEET.ID.eq(worksheetId))))
                    .orderBy(Tables.WORKSHEET.ID)
                    .limit(1)
                    .fetchOne()
                    .value1();

           return Lists.newArrayList(headers);
        });
    }

    @Override
    public List<String> getColumnHeaders(String spreadsheetId, String worksheetName) {
        return dsl.transactionResult((TransactionalCallable<List<String>>) configuration -> {
            final DSLContext innerDsl = DSL.using(configuration);

            long maxVersion = innerDsl.select(DSL.max(Tables.WORKSHEET.ID))
                    .from(Tables.WORKSHEET)
                    .where(Tables.WORKSHEET.SPREADSHEET_ID.eq(spreadsheetId)
                            .and(Tables.WORKSHEET.NAME.eq(worksheetName)))
                    .limit(1)
                    .fetchOne().value1();

            final String[] headers = innerDsl.select(Tables.WORKSHEET.COLUMN_HEADERS)
                    .from(Tables.WORKSHEET)
                    .where(Tables.WORKSHEET.VERSION.eq(maxVersion)
                            .and(Tables.WORKSHEET.SPREADSHEET_ID.eq(spreadsheetId)
                                    .and(Tables.WORKSHEET.NAME.eq(worksheetName))))
                    .orderBy(Tables.WORKSHEET.ID)
                    .limit(1)
                    .fetchOne()
                    .value1();

            return Lists.newArrayList(headers);
        });
    }

    @Override
    public Tuple2<Integer, Integer> getDimensions(String spreadsheetId, String worksheetName) {
        return null;
    }

    @Override
    public Tuple2<Integer, Integer> getDimensions(String spreadsheetId, int worksheetId) {
        return null;
    }

    @Override
    public SearchResults<JsonNode> findRows(String spreadsheetId, int worksheetId, Map<String, Object> params, Page page) {
        return dsl.transactionResult(configuration -> {
            final DSLContext innerDsl = DSL.using(configuration);

            long maxVersion = innerDsl.select(DSL.max(Tables.WORKSHEET.ID))
                    .from(Tables.WORKSHEET)
                    .where(Tables.WORKSHEET.SPREADSHEET_ID.eq(spreadsheetId)
                            .and(Tables.WORKSHEET.ID.eq(worksheetId)))
                    .limit(1)
                    .fetchOne().value1();

            Condition condition = Tables.WORKSHEET.SPREADSHEET_ID.eq(spreadsheetId)
                    .and(Tables.WORKSHEET.ID.eq(worksheetId)).and(Tables.WORKSHEET.VERSION.eq(maxVersion));

            if (null != params && !params.isEmpty()) {
                condition = condition.and(PgJsonBUtils.jsonContains(Tables.WORKSHEET.ROW_JSONB, params));
            }

            int totalResults = innerDsl.select(DSL.count(Tables.WORKSHEET.PK))
                    .from(Tables.WORKSHEET)
                    .where(condition)
                    .fetchOne()
                    .value1();

            List<JsonNode> results = innerDsl.select(Tables.WORKSHEET.ROW_JSONB)
                    .from(Tables.WORKSHEET)
                    .where(condition)
                    .orderBy(Tables.WORKSHEET.ROW_INDEX)
                    .offset(page.offset())
                    .limit(page.size())
                    .fetch(Tables.WORKSHEET.ROW_JSONB);

            return SearchResults.searchResults(totalResults, results);
        });
    }

    @Override
    public SearchResults<JsonNode> findRows(String spreadsheetId, String worksheetName, Map<String, Object> params, Page page) {
        return dsl.transactionResult(configuration -> {
            final DSLContext innerDsl = DSL.using(configuration);

            long maxVersion = innerDsl.select(DSL.max(Tables.WORKSHEET.ID))
                    .from(Tables.WORKSHEET)
                    .where(Tables.WORKSHEET.SPREADSHEET_ID.eq(spreadsheetId)
                            .and(Tables.WORKSHEET.NAME.eq(worksheetName)))
                    .limit(1)
                    .fetchOne().value1();

            Condition condition = Tables.WORKSHEET.SPREADSHEET_ID.eq(spreadsheetId)
                    .and(Tables.WORKSHEET.NAME.eq(worksheetName)).and(Tables.WORKSHEET.VERSION.eq(maxVersion));

            if (null != params && !params.isEmpty()) {
                condition = condition.and(PgJsonBUtils.jsonContains(Tables.WORKSHEET.ROW_JSONB, params));
            }

            int totalResults = innerDsl.select(DSL.count(Tables.WORKSHEET.PK))
                    .from(Tables.WORKSHEET)
                    .where(condition)
                    .fetchOne()
                    .value1();

            List<JsonNode> results = innerDsl.select(Tables.WORKSHEET.ROW_JSONB)
                    .from(Tables.WORKSHEET)
                    .where(condition)
                    .orderBy(Tables.WORKSHEET.ROW_INDEX)
                    .offset(page.offset())
                    .limit(page.size())
                    .fetch(Tables.WORKSHEET.ROW_JSONB);

            return SearchResults.searchResults(totalResults, results);
        });
    }

}
