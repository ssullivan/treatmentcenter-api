package com.github.ssullivan.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.ssullivan.db.ISpreadsheetDao;
import com.github.ssullivan.db.IWorksheetDao;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.google.inject.Inject;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class RecoveryHousingController implements IRecoveryHousingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryHousingController.class);

    private ISpreadsheetDao spreadsheetDao;
    private IWorksheetDao worksheetDao;
    private GoogleSheetsReader googleSheetsReader;

    @Inject
    public RecoveryHousingController(IWorksheetDao worksheetDao,
                                     ISpreadsheetDao spreadsheetDao,
                                     GoogleSheetsReader googleSheetsReader) {
        this.worksheetDao = worksheetDao;
        this.spreadsheetDao = spreadsheetDao;
        this.googleSheetsReader = googleSheetsReader;
    }

    @Override
    public void syncSpreadsheet(String spreadsheetId, boolean publish) throws IOException {
        try {
            this.googleSheetsReader.importSpreadsheet(spreadsheetId);
            if (publish) {
                this.spreadsheetDao.publish(spreadsheetId);
            } else {
                this.spreadsheetDao.unpublish(spreadsheetId);
            }
        } catch (DataAccessException e) {
            throw new IOException("Failed to write to database. SpradsheetId was " + spreadsheetId, e);
        }
    }

    @Override
    public SearchResults<JsonNode> listAll(Map<String, String> params, Page page) throws IOException {
        try {
            return SearchResults.searchResults(worksheetDao.count(params), worksheetDao.listAll(params, page));
        } catch (DataAccessException e) {
            throw new IOException("Failed to query database", e);
        }
    }
}
