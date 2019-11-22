package com.github.ssullivan.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.RecoveryHousingSearchRequest;
import com.github.ssullivan.model.SearchResults;

import java.io.IOException;
import java.util.Map;

public interface IRecoveryHousingController {

    void syncSpreadsheet(final String spreadsheetId, final boolean publish) throws IOException;

    SearchResults<JsonNode> listAll(final RecoveryHousingSearchRequest searchRequest, Page page) throws IOException;
}
