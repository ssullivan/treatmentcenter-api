package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.db.ISpreadsheetDao;
import com.github.ssullivan.db.psql.Tables;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.OffsetDateTime;

public class SpreadsheetDao implements ISpreadsheetDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorksheetDao.class);

    private final DSLContext dsl;

    @Inject
    public SpreadsheetDao(final DSLContext dslContext) {
        this.dsl = dslContext;
    }

    @Override
    public boolean isPublished(String spreadsheetId) {
        return dsl.select(Tables.SPREADSHEET.PUBLISHED)
                .from(Tables.SPREADSHEET)
                .where(Tables.SPREADSHEET.ID.eq(spreadsheetId))
                .fetchOptional(Tables.SPREADSHEET.PUBLISHED)
                .orElse(false);
    }

    @Override
    public void publish(String spreadsheetId) {
        dsl.update(Tables.SPREADSHEET)
                .set(Tables.SPREADSHEET.PUBLISHED, true)
                .execute();
    }

    @Override
    public void unpublish(String spreadsheetId) {
        dsl.update(Tables.SPREADSHEET)
                .set(Tables.SPREADSHEET.PUBLISHED, false)
                .execute();
    }

    @Override
    public void upsert(Spreadsheet spreadsheet) {
        final OffsetDateTime now = OffsetDateTime.now();

        dsl.insertInto(Tables.SPREADSHEET)
                .set(Tables.SPREADSHEET.ID, spreadsheet.getSpreadsheetId())
                .set(Tables.SPREADSHEET.NAME, spreadsheet.getProperties().getTitle())
                .set(Tables.SPREADSHEET.PUBLISHED, false)
                .set(Tables.SPREADSHEET.NUM_SHEETS, spreadsheet.getSheets().size())
                .set(Tables.SPREADSHEET.LAST_UPDATED, now)
                .onDuplicateKeyUpdate()
                .set(Tables.SPREADSHEET.ID, spreadsheet.getSpreadsheetId())
                .set(Tables.SPREADSHEET.NAME, spreadsheet.getProperties().getTitle())
                .set(Tables.SPREADSHEET.NUM_SHEETS, spreadsheet.getSheets().size())
                .set(Tables.SPREADSHEET.LAST_UPDATED, now)
                .execute();
    }

}
