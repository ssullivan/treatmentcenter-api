package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.db.IWorksheetDao;
import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.sheets.SheetRow;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public int upsertBatch(String spreadsheetId, Sheet sheet, List<SheetRow> rows) {
        Objects.requireNonNull(spreadsheetId, "spreadsheetId must not be null");
        Objects.requireNonNull(sheet, "sheet must not be null");
        Objects.requireNonNull(sheet.getProperties(), "sheet properties must not be null");
        Objects.requireNonNull(sheet.getProperties().getSheetId(), "sheetId must not be null");
        Objects.requireNonNull(rows, "rows must not be null");

        /**
         * It's faster to do multiple inserts in a single transaction. The reason that
         * I'm not doing one big multi row insert is because the jooq api doesn't (easily)
         * support the syntax for onDuplicateKeyUpdate() for that use case.
         */
        return dsl.transactionResult(configuration -> {
            final DSLContext innerDsl = DSL.using(configuration);
            int total = 0;
            for (SheetRow sheetRow : rows) {
                final JsonNode data = objectMapper.convertValue(sheetRow.getCells(), JsonNode.class);

                total += innerDsl.insertInto(Tables.WORKSHEET)
                        .set(Tables.WORKSHEET.SPREADSHEET_ID, spreadsheetId)
                        .set(Tables.WORKSHEET.NAME, sheet.getProperties().getTitle())
                        .set(Tables.WORKSHEET.ID, sheet.getProperties().getSheetId())
                        .set(Tables.WORKSHEET.COLUMN_HEADERS, sheetRow.getHeaders())
                        .set(Tables.WORKSHEET.ROW_JSONB, data)
                        .set(Tables.WORKSHEET.ROW_INDEX, sheetRow.getRowIndex())
                        .onDuplicateKeyUpdate()
                        .set(Tables.WORKSHEET.SPREADSHEET_ID, spreadsheetId)
                        .set(Tables.WORKSHEET.NAME, sheet.getProperties().getTitle())
                        .set(Tables.WORKSHEET.ID, sheet.getProperties().getSheetId())
                        .set(Tables.WORKSHEET.COLUMN_HEADERS, sheetRow.getHeaders())
                        .set(Tables.WORKSHEET.ROW_JSONB, data)
                        .set(Tables.WORKSHEET.ROW_INDEX, sheetRow.getRowIndex())
                        .execute();
            }
            return total;
        });
    }

    @Override
    public int deleteRows(String spreadsheetId, int sheetId, Range<Integer> rowIndexRange) {
        return dsl.deleteFrom(Tables.WORKSHEET)
                .where(Tables.WORKSHEET.SPREADSHEET_ID.eq(spreadsheetId).and(Tables.WORKSHEET.ID.eq(sheetId))
                        .and(toRangeCondition(Tables.WORKSHEET.ROW_INDEX, rowIndexRange)))
                .execute();
    }

    @Override
    public long count(Map<String, String> params) {
        Objects.requireNonNull(params, "params must not be null");

        Condition whereCondition = Tables.SPREADSHEET.PUBLISHED.eq(true);
        if (!params.isEmpty()) {
            whereCondition = whereCondition.and(PgJsonBUtils.jsonContains(Tables.WORKSHEET.ROW_JSONB, params);
        }

        return dsl.select(DSL.count())
                .from(Tables.WORKSHEET)
                .innerJoin(Tables.SPREADSHEET).on(Tables.SPREADSHEET.ID.eq(Tables.WORKSHEET.SPREADSHEET_ID))
                .where(whereCondition)
                .fetchOne(0, Long.class);
    }

    @Override
    public List<JsonNode> listAll(Map<String, String> params, Page page) {
        Objects.requireNonNull(params, "params must not be null");
        Objects.requireNonNull(page, "page must not be null");

        Condition whereCondition = Tables.SPREADSHEET.PUBLISHED.eq(true);
        if (!params.isEmpty()) {
            whereCondition = whereCondition.and(PgJsonBUtils.jsonContains(Tables.WORKSHEET.ROW_JSONB, params);
        }

        return dsl.select(Tables.WORKSHEET.ROW_JSONB)
                .from(Tables.WORKSHEET)
                .innerJoin(Tables.SPREADSHEET).on(Tables.SPREADSHEET.ID.eq(Tables.WORKSHEET.SPREADSHEET_ID))
                .where(whereCondition)
                .orderBy(Tables.WORKSHEET.ROW_INDEX)
                .offset(page.offset())
                .limit(page.size())
                .fetch(Tables.WORKSHEET.ROW_JSONB);
    }

    @Override
    public List<JsonNode> listAll(String spreadsheetId, int sheetId, Page page) {
        Objects.requireNonNull(spreadsheetId, "spreadsheetId must not be null");
        return dsl.select(Tables.WORKSHEET.ROW_JSONB)
                .from(Tables.WORKSHEET)
                .where(Tables.WORKSHEET.SPREADSHEET_ID.eq(spreadsheetId).and(Tables.WORKSHEET.ID.eq(sheetId)))
                .orderBy(Tables.WORKSHEET.ROW_INDEX)
                .offset(page.offset())
                .limit(page.size())
                .fetch(Tables.WORKSHEET.ROW_JSONB);
    }

    @Override
    public List<JsonNode> listAll(String spreadsheetId, int sheetId) {
        Objects.requireNonNull(spreadsheetId, "spreadsheetId must not be null");
        return dsl.select(Tables.WORKSHEET.ROW_JSONB)
                .from(Tables.WORKSHEET)
                .where(Tables.WORKSHEET.SPREADSHEET_ID.eq(spreadsheetId).and(Tables.WORKSHEET.ID.eq(sheetId)))
                .fetch(Tables.WORKSHEET.ROW_JSONB);
    }

    @Override
    public List<JsonNode> listAll(String spreadsheetId, String title) {
        Objects.requireNonNull(spreadsheetId, "spreadsheetId must not be null");
        Objects.requireNonNull(title, "title must not be null");

        return dsl.select(Tables.WORKSHEET.ROW_JSONB)
                .from(Tables.WORKSHEET)
                .where(Tables.WORKSHEET.SPREADSHEET_ID.eq(spreadsheetId).and(Tables.WORKSHEET.NAME.eq(title)))
                .fetch(Tables.WORKSHEET.ROW_JSONB);
    }

    private static Condition toRangeCondition(Field<Integer> field, Range<Integer> range) {

        if (range.hasLowerBound() && !range.hasUpperBound()) {
            return toLowerBoundType(field, range.lowerBoundType(), range.lowerEndpoint());
        }
        else if (!range.hasLowerBound() && range.hasUpperBound()) {
            return toUpperBoundType(field, range.upperBoundType(), range.upperEndpoint());
        }
        else if (range.hasLowerBound()) {
            return toLowerBoundType(field, range.lowerBoundType(), range.lowerEndpoint())
                    .and(toUpperBoundType(field, range.upperBoundType(), range.upperEndpoint()));
        }
        else {
            return DSL.condition(false);
        }
    }

    private static Condition toUpperBoundType(Field<Integer> field, BoundType boundType, int endpoint) {
        if (BoundType.CLOSED.equals(boundType)) {
            return field.lessOrEqual(endpoint);
        }
        return field.lessThan(endpoint);
    }

    private static Condition toLowerBoundType(Field<Integer> field, BoundType boundType, int endpoint) {
        if (BoundType.CLOSED.equals(boundType)) {
            return field.greaterOrEqual(endpoint);
        }
        return field.greaterThan(endpoint);
    }


}
