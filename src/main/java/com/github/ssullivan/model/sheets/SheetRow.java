package com.github.ssullivan.model.sheets;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SheetRow {
    private LinkedHashMap<String, Object> cells;
    private int rowIndex;

    public SheetRow(LinkedHashMap<String, Object> cells, int rowIndex) {
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
}
