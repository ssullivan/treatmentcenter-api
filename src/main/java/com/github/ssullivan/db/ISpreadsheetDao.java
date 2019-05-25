package com.github.ssullivan.db;


import com.google.api.services.sheets.v4.model.Spreadsheet;

public interface ISpreadsheetDao {
    boolean isPublished(final String spreadsheetId);

    void publish(final String spreadsheetId);

    void unpublish(final String spreadsheetId);

    void upsert(final Spreadsheet spreadsheet);
}
