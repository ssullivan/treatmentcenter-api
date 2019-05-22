package com.github.ssullivan.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.collections.Tuple2;

import java.util.List;
import java.util.Map;

public interface IWorksheetDao {

    /**
     * Every update from the Google Spreadheet plugin will increment the version counter.
     *
     * @return the current version (most recent version change)
     */
    long getLatestVersion(final String spreadsheetId, final int worksheetId);

    List<JsonNode> listWorksheets(Page page);

    void deleteOldVersions();

    /**
     * Returns an in-order list of the worksheet column headers.
     *
     * @param spreadsheetId the id of the spreadsheet
     * @param worksheetId the id of the worksheet in the spreadsheet
     * @return the column headers
     */
    List<String> getColumnHeaders(final String spreadsheetId, final int worksheetId);

    /**
     * Returns an in-order list of the worksheet column headers.
     *
     * @param spreadsheetId the id of the spreadsheet
     * @param worksheetName the name of the worksheet in the spreadsheet
     * @return the column headers
     */
    List<String> getColumnHeaders(final String spreadsheetId, final String worksheetName);

    void addRows(final String spreadsheetId, final String worksheetName)

    /**
     * Returns the dimensions of the spreadsheet (rows, cols).
     *
     * @param spreadsheetId the id of the spreadsheet
     * @param worksheetName the name of the worksheet in the spreadsheet
     * @return the column headers
     */
    Tuple2<Integer, Integer> getDimensions(final String spreadsheetId, final String worksheetName);

    Tuple2<Integer, Integer> getDimensions(final String spreadsheetId, final int worksheetId);

    /**
     * Find rows in the spreadsheet.
     *
     * @param spreadsheetId the id of the spreadsheet
     * @param worksheetId the id of the worksheet in the spreadsheet
     * @param params a list of column headers and values to filter by (supports == comparisons only)
     * @param page the page
     * @return the rows of the spreadsheet ( fieldName => fieldValue )
     */
    SearchResults<JsonNode> findRows(final String spreadsheetId,
                                     final int worksheetId,
                                     final Map<String, Object> params,
                                     Page page);

    SearchResults<JsonNode> findRows(final String spreadsheetId,
                                     final String worksheetName,
                                     final Map<String, Object> params,
                                     Page page);

    /**
     * Find rows in the spreadsheet. Defaults to the first sheet.
     *
     * @param spreadsheetId the id of the spreadsheet
     * @param params a list of column headers and values to filter by (supports == comparisons only)
     * @param page the page
     * @return the rows of the spreadsheet ( fieldName => fieldValue )
     */
    default SearchResults<JsonNode> findRows(final String spreadsheetId,
                                     final Map<String, Object> params,
                                     Page page) {
        return findRows(spreadsheetId, 0, params, page);
    }
}
