package com.github.ssullivan.core;

import com.github.ssullivan.db.ISpreadsheetDao;
import com.github.ssullivan.db.IWorksheetDao;
import com.github.ssullivan.model.sheets.SheetRow;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.Range;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

public class GoogleSheetsReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleSheetsReader.class);

    private IWorksheetDao worksheetDao;
    private ISpreadsheetDao spreadsheetDao;
    private Sheets client;


    @Inject
    public GoogleSheetsReader(IWorksheetDao worksheetDao,
                              ISpreadsheetDao spreadsheetDao,
                              Sheets client) {

        this.worksheetDao = worksheetDao;
        this.spreadsheetDao = spreadsheetDao;
        this.client = client;
    }

    public void importSpreadsheet(final String spreadsheetId) throws IOException {
        final Sheets.Spreadsheets.Get spreadsheetGet = client.spreadsheets().get(spreadsheetId);

        this.spreadsheetDao.upsert(spreadsheetGet.execute());

        List<Sheet> sheets = spreadsheetGet.execute().getSheets();
        List<String> headers = getColumnHeaders(client, spreadsheetId, sheets.get(0));

        // Controls how many records we fetch from the API at once
        int fetchSize = 1000;
        int totalRows = sheets.get(0).getProperties().getGridProperties().getRowCount();

        // Controls how many records we insert into the database at once
        int batchSize = 100;
        int rowCounter = 0;
        int emptyRowIndex = 0;

        // Fetch ValueRanges in batches
        final List<SheetRow> aggBatch = new ArrayList<>();
        for (int i = 1; i <= totalRows; i += fetchSize) {
            String range = toRange(sheets.get(0), i, fetchSize);
            try {
                final ValueRange valueRange = client.spreadsheets()
                        .values()
                        .get(spreadsheetId, range)
                        .setMajorDimension("ROWS")
                        .execute();

                if (valueRange != null && valueRange.getValues() != null) {
                    for (List<Object> row : valueRange.getValues()) {
                        // We need to make sure there is parity with the column headers
                        if (row.size() > headers.size()) {
                            headers = getColumnHeaders(client, spreadsheetId, sheets.get(0));
                        }

                        aggBatch.add(new SheetRow(toMap(headers, row), rowCounter));
                        rowCounter++;
                    }

                    if ((aggBatch.size() % batchSize) == 0) {
                        worksheetDao.upsertBatch(spreadsheetId, sheets.get(0), aggBatch);
                        aggBatch.clear();
                    }
                }
                else {
                    LOGGER.info("Found empty row at row: {}", rowCounter);
                    emptyRowIndex = rowCounter;
                    break;
                }
            } catch (IOException e) {
                LOGGER.error("Failed to get range: {} from spreadsheet: {}", range, spreadsheetId, e);
                break;
            }
        }

        if (! aggBatch.isEmpty()) {
            worksheetDao.upsertBatch(spreadsheetId, sheets.get(0), aggBatch);
        }

        // Delete anything starting at the empty row
        int rowsDeleted = worksheetDao.deleteRows(spreadsheetId, sheets.get(0), Range.closedOpen(emptyRowIndex, Integer.MAX_VALUE));
        LOGGER.info("Deleted {} rows", rowsDeleted);
    }


    private static LinkedHashMap<String, Object> toMap(final List<String> headers, final List<Object> values) {
        if (headers.size() < values.size()) {
            throw new IllegalArgumentException("Header Length must be the same as values length");
        }

        LinkedHashMap<String, Object> map = new LinkedHashMap<>(values.size());
        int maxI = values.size();
        for (int i = 0; i < maxI; ++i) {
            map.put(headers.get(i), values.get(i));
        }

        return map;
    }

    private static List<String> getColumnHeaders(Sheets client, final String spreadsheetId, Sheet sheet) throws IOException {
        List<List<Object>> rows = client.spreadsheets().values().get(spreadsheetId, toRange(sheet, 1))
                .setMajorDimension("ROWS")
                .execute()
                .getValues();

        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>(0);
        }

        List<Object> firstRow = rows.get(0);
        List<String> headerRow = new ArrayList<>(firstRow.size());

        for (Object cell : firstRow) {
            headerRow.add(cell.toString());
        }

        return Collections.unmodifiableList(headerRow);
    }

    private static String toRange(Sheet sheet, final int offset, final int limit) {
        return sheet.getProperties().getTitle() + "!" + offset + ":" + (offset + limit);
    }

    private static String toRange(Sheet sheet, final int rows) {
        return sheet.getProperties().getTitle() + "!A1:" +
                getExcelColumnName(sheet.getProperties().getGridProperties().getColumnCount()) + rows;
    }


    private static String getExcelColumnName(int number) {
        final StringBuilder sb = new StringBuilder();

        int num = number - 1;
        while (num >=  0) {
            int numChar = (num % 26)  + 65;
            sb.append((char)numChar);
            num = (num  / 26) - 1;
        }
        return sb.reverse().toString();
    }

}
