package com.github.ssullivan.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.sheets.SheetRow;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.common.collect.Range;

import java.util.List;
import java.util.Map;

public interface IWorksheetDao {

    /**
     * Upsets the rows into the db.
     *
     * @param spreadsheetId the id
     * @param sheet
     * @param rows
     * @return number of rows inserted
     */
    int upsertBatch(final String spreadsheetId,
                    final Sheet sheet,
                     final List<SheetRow> rows);

    default int deleteRows(final String spreadsheetId, final Sheet sheet, final Range<Integer> rowIndexRange) {
        return deleteRows(spreadsheetId, sheet.getProperties().getSheetId(), rowIndexRange);
    }

    int deleteRows(final String spreadsheetId, final int sheetId, final Range<Integer> rowIndexRange);

    long count(final Map<String, String> params);

    List<JsonNode> listAll(final Map<String, String> params, final Page page);

    List<JsonNode> listAll(final String spreadsheetId, final int sheetId, Page page);

    List<JsonNode> listAll(final String spreadsheetId, final int sheetId);

    List<JsonNode> listAll(final String spreadsheetId, final String title);
}
