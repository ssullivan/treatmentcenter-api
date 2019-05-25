package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.db.IWorksheetDao;
import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.model.sheets.SheetRow;
import com.google.api.services.sheets.v4.model.Sheet;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
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
}
