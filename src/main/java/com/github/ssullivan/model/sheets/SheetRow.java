package com.github.ssullivan.model.sheets;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class SheetRow implements Serializable {
    private LinkedHashMap<String, Object> cells;
    private int rowIndex;

    public SheetRow(LinkedHashMap<String, Object> cells, int rowIndex) {
        Objects.requireNonNull(cells, "cells must not be null");
        this.cells = cells;
        this.rowIndex = rowIndex;
    }

    public Map<String, Object> getCells() {
        return Collections.unmodifiableMap(cells);
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
