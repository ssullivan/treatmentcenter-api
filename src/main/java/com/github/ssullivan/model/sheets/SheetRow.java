package com.github.ssullivan.model.sheets;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SheetRow implements Serializable {
    private static final Splitter SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

    private String spreadsheetId;
    private LinkedHashMap<String, Object> cells;
    private int rowIndex;

    public SheetRow(String spreadsheetId, LinkedHashMap<String, Object> cells, int rowIndex) {
        Objects.requireNonNull(cells, "cells must not be null");
        this.spreadsheetId = spreadsheetId;
        this.cells = cells;
        this.rowIndex = rowIndex;
    }

    public Map<String, Object> getCells() {
        return Collections.unmodifiableMap(cells);
    }

    public <T> T getValue(String name) {
        return (T) cells.get(name);
    }

    public String getStringValue(String name) {
        Object value = cells.get(name);
        if (value == null) return null;
        if (value instanceof String)
            return (String) value;
        else
            return value.toString();
    }

    public String[] getStringArray(String name) {
        Object value = cells.get(name);
        if (value instanceof String) {
            return Iterables.toArray(SPLITTER.split((String) value), String.class);
        }
        else if (value instanceof List) {
            return (String[]) ((List) value).stream().map(Object::toString).toArray();
        }
        else if (value instanceof Set) {
            return (String[]) ((Set) value).stream().map(Object::toString).toArray();
        }
        return null;
    }

    public Integer getIntValue(String name) {
        Object value = cells.get(name);
        if (value == null) return null;
        if (value instanceof Integer)
            return (Integer) value;
        else if (value instanceof Long)
            return ((Long) value).intValue();
        else if (value instanceof String)
            return Integer.parseInt((String) value, 10);
        return 0;
    }

    public Long getLongValue(String name) {
        Object value = cells.get(name);
        if (value == null) return null;
        if (value instanceof Long)
            return (Long) value;
        else if (value instanceof Integer)
            return ((Integer) value).longValue();
        else if (value instanceof String)
            return Long.parseLong((String) value, 10);
        return 0L;
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public String[] getHeaders() {
        String[] headers = new String[cells.keySet().size()];
        return cells.keySet().toArray(headers);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SheetRow sheetRow = (SheetRow) o;
        return rowIndex == sheetRow.rowIndex &&
                cells.equals(sheetRow.cells);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cells, rowIndex);
    }
}
