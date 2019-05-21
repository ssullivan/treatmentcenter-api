package com.github.ssullivan.db;

public interface ISpreadsheetDao {
    boolean isPublished(final String spreadsheetId);

    void publish(final String spreadsheetId);
}
