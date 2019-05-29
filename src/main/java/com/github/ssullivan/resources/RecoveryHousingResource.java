package com.github.ssullivan.resources;

import com.github.ssullivan.auth.RequireApiKey;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.github.ssullivan.core.IRecoveryHousingController;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.glassfish.jersey.server.ManagedAsync;
import org.jooq.Require;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

@Path("/recovery")
public class RecoveryHousingResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryHousingResource.class);

  private static final String SPREADSHEET_ID = "spreadsheetId";
  private static final String PUBLISH = "publish";

  private IRecoveryHousingController recoveryHousingController;

  @Inject
  public RecoveryHousingResource(IRecoveryHousingController recoveryHousingController) {
    this.recoveryHousingController = recoveryHousingController;
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ManagedAsync
  @Path("/sync")
  @RequireApiKey
  public void syncGoogleSheets(Map<String, String> spreadsheetSyncRequest, @Suspended AsyncResponse asyncResponse) {
    if (spreadsheetSyncRequest == null || spreadsheetSyncRequest.isEmpty()) {
      asyncResponse.resume(Response.status(400).entity(ImmutableMap.of("message", "Invalid sync request. Please specify a " + SPREADSHEET_ID + ", and " + PUBLISH + " status of {true|false}")).build());
    }

    if (null == spreadsheetSyncRequest.get(SPREADSHEET_ID)) {
      asyncResponse.resume(Response.status(400).entity(ImmutableMap.of("message", "Invalid sync request. Please specify a " + SPREADSHEET_ID )).build());
    }

    String spreadsheetId = spreadsheetSyncRequest.get(SPREADSHEET_ID);
    Boolean publish = Boolean.valueOf(spreadsheetSyncRequest.get(PUBLISH));

    try {
      recoveryHousingController.syncSpreadsheet(spreadsheetId, publish);
      asyncResponse.resume(Response.noContent().build());
    } catch (IOException e) {
      LOGGER.error("Failed to sync google spreadsheet {}", spreadsheetId, e);
      asyncResponse.resume(Response.status(400).entity(ImmutableMap.of("message", "Failed to sync spreadsheet.")));
    }
  }

  @GET
  public void search()  {

  }
}
