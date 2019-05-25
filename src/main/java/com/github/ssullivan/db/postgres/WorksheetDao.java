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
}
