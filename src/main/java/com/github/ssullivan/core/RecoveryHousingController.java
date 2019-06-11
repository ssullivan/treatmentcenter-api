package com.github.ssullivan.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.ssullivan.db.IRecoveryHousingDao;
import com.github.ssullivan.db.ISpreadsheetDao;
import com.github.ssullivan.db.IWorksheetDao;
import com.github.ssullivan.db.psql.tables.records.RecoveryHousingRecord;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.RecoveryHousingSearchRequest;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.sheets.SheetRow;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class RecoveryHousingController implements IRecoveryHousingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryHousingController.class);

    private IRecoveryHousingDao housingDao;
    private GoogleSheetsReader googleSheetsReader;

    @Inject
    public RecoveryHousingController(IRecoveryHousingDao dao,
                                     GoogleSheetsReader googleSheetsReader) {

        this.housingDao = dao;
        this.googleSheetsReader = googleSheetsReader;
    }

    @Override
    public void syncSpreadsheet(String spreadsheetId, boolean publish) throws IOException {
        try {
            SheetHandler handler = new SheetHandler(spreadsheetId, housingDao);
            this.googleSheetsReader.importSpreadsheet(spreadsheetId, handler::handle);
            handler.deleteOldVersions();
        } catch (DataAccessException e) {
            throw new IOException("Failed to write to database. SpradsheetId was " + spreadsheetId, e);
        }
    }

    @Override
    public SearchResults<JsonNode> listAll(RecoveryHousingSearchRequest searchRequest,
        Page page) throws IOException {
        return null;
    }

    private List<JsonNode> toJsonNodes(List<RecoveryHousingRecord> records) {
        return null;
    }
    private static class SheetHandler {
        private String spreadsheetId;
        private IRecoveryHousingDao dao;
        private Long version;

        SheetHandler(String spreadsheetId, IRecoveryHousingDao dao) {
            this.spreadsheetId = spreadsheetId;
            version = System.currentTimeMillis();
            this.dao = dao;
        }

        public boolean handle(List<SheetRow> rows) {
            List<RecoveryHousingRecord> records = new ArrayList<>(rows.size());
            for (SheetRow sheetRow : rows) {
                RecoveryHousingRecord record = new RecoveryHousingRecord()
                    .setFeedVersion(version)
                    .setName(sheetRow.getStringValue(RecoveryHousingConstants.RESIDENCE_NAME))
                    .setState(sheetRow.getStringValue(RecoveryHousingConstants.STATE))
                    .setStreet(sheetRow.getStringValue(RecoveryHousingConstants.STREET_ADDRESS))
                    .setCity(sheetRow.getStringValue(RecoveryHousingConstants.CITY))
                    .setPostalcode(sheetRow.getStringValue(RecoveryHousingConstants.ZIP_CODE))
                    .setContactPhonenumber(
                        sheetRow.getStringValue(RecoveryHousingConstants.PHONE_NUMBER))
                    .setWehsite(sheetRow.getStringValue(RecoveryHousingConstants.WEBSITE))
                    .setServes(sheetRow.getStringArray(RecoveryHousingConstants.GENDER))
                    .setFeedName(sheetRow.getSpreadsheetId())
                    .setFeedRecordId(sheetRow.getLongValue("ID"));
                records.add(record);
            }
            this.dao.upsert(records);
            return true;
        }

        void deleteOldVersions() {
            this.dao.deleteByVersion(spreadsheetId, Range.lessThan(version));
        }
    }
}
