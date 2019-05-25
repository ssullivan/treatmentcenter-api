package com.github.ssullivan.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.ssullivan.model.sheets.SheetRow;
import com.google.api.services.sheets.v4.model.Sheet;

import java.util.List;

public interface IWorksheetDao {

    int upsertBatch(final String spreadsheetId,
                    final Sheet sheet,
                     final List<SheetRow> rows);


    List<JsonNode> listAll(final String spreadsheetId, final int sheetId);

    List<JsonNode> listAll(final String spreadsheetId, final String title);
}
