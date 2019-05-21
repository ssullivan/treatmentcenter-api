package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.db.IWorksheetDao;
import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.collections.Tuple2;
import org.jooq.DSLContext;
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
        return dsl.select(DSL.max(Tables.SHEET_ROWS.VERSION))
                .where(Tables.SHEET_ROWS.SPREADSHEET_ID.eq(spreadsheetId)
                        .and(Tables.SHEET_ROWS.SHEET_ID.eq(worksheetId)))
                .limit(1)
                .fetchOne()
                .value1();
    }

    @Override
    public List<JsonNode> listWorksheets(Page page) {
        return null;
    }

    @Override
    public void deleteOldVersions() {

    }

    @Override
    public List<String> getColumnHeaders(String spreadsheetId, int worksheetId) {
        return null;
    }

    @Override
    public List<String> getColumnHeaders(String spreadsheetId, String worksheetName) {
        return null;
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
        return null;
    }

    @Override
    public SearchResults<JsonNode> findRows(String spreadsheetId, String worksheetName, Map<String, Object> params, Page page) {
        return null;
    }
}
