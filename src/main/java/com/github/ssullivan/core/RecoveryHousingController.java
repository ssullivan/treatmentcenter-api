package com.github.ssullivan.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.ssullivan.db.IRecoveryHousingDao;
import com.github.ssullivan.db.psql.tables.records.RecoveryHousingRecord;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.RecoveryHousingSearchRequest;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.sheets.SheetRow;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecoveryHousingController implements IRecoveryHousingController {

  private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryHousingController.class);

  private IRecoveryHousingDao housingDao;
  private GoogleSheetsReader googleSheetsReader;

  @Inject
  public RecoveryHousingController(IRecoveryHousingDao dao,
      GoogleSheetsReader googleSheetsReader) {

    this.housingDao = dao;
    this.googleSheetsReader = googleSheetsReader;
  }

  @Override
  public void syncSpreadsheet(String spreadsheetId, boolean publish) throws IOException {
    try {
      SheetHandler handler = new SheetHandler(spreadsheetId, housingDao);
      this.googleSheetsReader.importSpreadsheet(spreadsheetId, handler::handle);
      handler.deleteOldVersions();
    } catch (DataAccessException e) {
      throw new IOException("Failed to write to database. SpradsheetId was " + spreadsheetId, e);
    }
  }

  @Override
  public SearchResults<JsonNode> listAll(RecoveryHousingSearchRequest searchRequest,
      Page page) throws IOException {
    return null;
  }

  private List<JsonNode> toJsonNodes(List<RecoveryHousingRecord> records) {
    return null;
  }

  private static class SheetHandler {

    private String spreadsheetId;
    private IRecoveryHousingDao dao;
    private Long version;

    SheetHandler(String spreadsheetId, IRecoveryHousingDao dao) {
      this.spreadsheetId = spreadsheetId;
      version = System.currentTimeMillis();
      this.dao = dao;
    }

    public boolean handle(List<SheetRow> rows) {
      List<RecoveryHousingRecord> records = new ArrayList<>(rows.size());
      for (SheetRow sheetRow : rows) {
        Visibility visibility = new Visibility(sheetRow);

        RecoveryHousingRecord record = new RecoveryHousingRecord()
            .setFeedVersion(version)
            .setState(sheetRow.getStringValue(RecoveryHousingConstants.STATE))
            .setCity(sheetRow.getStringValue(RecoveryHousingConstants.CITY))
            .setWehsite(sheetRow.getStringValue(RecoveryHousingConstants.WEBSITE))
            .setFeedName(sheetRow.getSpreadsheetId())
            .setFeedRecordId(sheetRow.getLongValue("ID"));

            if (visibility.canShareResidenceName()) {
              record.setName(sheetRow.getStringValue(RecoveryHousingConstants.RESIDENCE_NAME));
            }

            if (visibility.canShareAddress()) {
              record.setStreet(sheetRow.getStringValue(RecoveryHousingConstants.STREET_ADDRESS));
            }

            if (visibility.canShareZipCode()) {
              record.setPostalcode(sheetRow.getStringValue(RecoveryHousingConstants.ZIP_CODE);
            }

            if (visibility.canSharePhoneNumber()) {
              record.setContactPhonenumber(
                  sheetRow.getStringValue(RecoveryHousingConstants.PHONE_NUMBER));
            }

            if (visibility.canShareNumberOfBeds()) {
              record.setCapacity(sheetRow.getIntValue(RecoveryHousingConstants.CAPACITY));
            }
            if (visibility.canShareEmail()) {
              record.setContactEmail(sheetRow.getStringValue(RecoveryHousingConstants.RES_EMAIL));
            }

        records.add(record);
      }
      this.dao.upsert(records);
      return true;
    }

    void deleteOldVersions() {
      this.dao.deleteByVersion(spreadsheetId, Range.lessThan(version));
    }
  }


  private static class Visibility {
    private final Set<String> canBeShared;

    public Visibility(final SheetRow sheetRow) {
        this.canBeShared = Sets.newHashSet(sheetRow.getStringArray("Click all information you would like to share publicly"));
    }

    public boolean canShareAddress() {
      return canBeShared.contains("Street Address") || canBeShared.contains("all");
    }

    public boolean canShareZipCode() {
      return canBeShared.contains("Zip Code") || canBeShared.contains("all");
    }

    public boolean canSharePhoneNumber() {
      return canBeShared.contains("Phone Number") || canBeShared.contains("all");
    }

    public boolean canShareNumberOfBeds() {
      return canBeShared.contains("Number of Beds") || canBeShared.contains("all");
    }

    public boolean canShareResidenceName() {
      return canBeShared.contains("Residence Name") || canBeShared.contains("all");
    }

    public boolean canShareEmail() {
      return canBeShared.contains("Email Address") || canBeShared.contains("all");
    }
  }
}
