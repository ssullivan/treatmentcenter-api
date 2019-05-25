package com.github.ssullivan.db;

import com.github.ssullivan.model.sheets.SheetRow;
import com.google.api.services.sheets.v4.model.Sheet;

import java.util.List;

public interface IWorksheetDao {

    int upsertBatch(final String spreadsheetId,
                    final Sheet sheet,
                     final List<SheetRow> rows);


}
