package com.github.ssullivan.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.ssullivan.db.IWorksheetDao;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class RecoveryHousingController implements IRecoveryHousingController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryHousingController.class);

    private IWorksheetDao worksheetDao;
    private GoogleSheetsReader googleSheetsReader;

    @Inject
    public RecoveryHousingController(IWorksheetDao worksheetDao, GoogleSheetsReader googleSheetsReader) {
        this.worksheetDao = worksheetDao;
        this.googleSheetsReader = googleSheetsReader;
    }

    @Override
    public void syncSpreadsheet(String spreadsheetId, boolean publish) throws IOException {
        this.googleSheetsReader.importSpreadsheet(spreadsheetId);

    }

    @Override
    public SearchResults<JsonNode> listAll(Map<String, String> params, Page page) {
        return SearchResults.searchResults(worksheetDao.count(params), worksheetDao.listAll(params, page));
    }
}
