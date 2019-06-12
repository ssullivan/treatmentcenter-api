package com.github.ssullivan.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.ssullivan.auth.RequireApiKey;
import com.github.ssullivan.core.IRecoveryHousingController;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.RecoveryHousingSearchRequest;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.conditions.RangeCondition;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/recovery")
public class RecoveryHousingResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryHousingResource.class);

  static final String SPREADSHEET_ID = "spreadsheetId";
  static final String PUBLISH = "publish";
  private IRecoveryHousingController recoveryHousingController;

  @Inject
  public RecoveryHousingResource(IRecoveryHousingController recoveryHousingController) {
    this.recoveryHousingController = recoveryHousingController;

  }

  @POST
  @ManagedAsync
  @Path("/sync")
  @RequireApiKey
  public void syncGoogleSheets(Map<String, String> spreadsheetSyncRequest, @Suspended AsyncResponse asyncResponse) {
    asyncResponse.setTimeout(1, TimeUnit.SECONDS);
    asyncResponse.setTimeoutHandler(asyncResponse1 -> asyncResponse1.resume(Response.ok(ImmutableMap.of("message", "Processing of spreadsheet timedout.")).build()));

    if (spreadsheetSyncRequest == null || spreadsheetSyncRequest.isEmpty()) {
      asyncResponse.resume(Response.status(400).entity(ImmutableMap.of("message", "Invalid sync request. Please specify a " + SPREADSHEET_ID + ", and " + PUBLISH + " status of {true|false}")).build());
      return;
    }

    if (null == spreadsheetSyncRequest.get(SPREADSHEET_ID)) {
      asyncResponse.resume(Response.status(400).entity(ImmutableMap.of("message", "Invalid sync request. Please specify a " + SPREADSHEET_ID )).build());
      return;
    }

    String spreadsheetId = spreadsheetSyncRequest.get(SPREADSHEET_ID);
    boolean publish = Boolean.valueOf(spreadsheetSyncRequest.get(PUBLISH));

    try {
      recoveryHousingController.syncSpreadsheet(spreadsheetId, publish);
      asyncResponse.resume(Response.noContent().build());
    } catch (IOException e) {
      LOGGER.error("Failed to sync google spreadsheet {}", spreadsheetId, e);
      asyncResponse.resume(Response.status(400).entity(ImmutableMap.of("message", "Failed to sync spreadsheet.")));
    }
  }

  @GET
  @Path("/search")
  @ManagedAsync
  public void search(@QueryParam("postalCode") String postalCode,
                     @QueryParam("state") String state,
                     @QueryParam("city") String city,
                     @QueryParam("capacity") String capacity,

                     @ApiParam(value = "the number of results to skip", allowableValues = "range[0, 9999]")
                     @Min(0) @Max(9999) @DefaultValue("0")
                     @QueryParam("offset")
                     final int offset,

                     @ApiParam(value = "the number of results to return", allowableValues = "range[0, 9999]")
                     @Min(0) @Max(9999)
                     @DefaultValue("10")
                     @QueryParam("size") final int size,

                     @Suspended AsyncResponse asyncResponse)  {

    try {
      SearchResults<JsonNode> results = recoveryHousingController.listAll(new RecoveryHousingSearchRequest()
          .withCapacity(capacity != null ? new RangeParameter(capacity).getRange() : null)
          .withCity(city)
          .withZipcode(postalCode), Page.page(offset, size));

      asyncResponse.resume(Response.ok(results).build());
    } catch (IOException e) {
      LOGGER.error("Failed to search for recovery hosing locations with", e);
      asyncResponse.resume(Response.status(500).entity(ImmutableMap.of("message", "Search failed")).build());
    }
  }


  static class RangeParameter {
    private static final ObjectReader READER = new ObjectMapper().readerFor(RangeCondition.class);
    private RangeCondition range;

    public RangeParameter(String json) throws WebApplicationException {
      try {
        this.range = READER.readValue(json);
      } catch (IOException e) {
        try {
          String decoded = URLDecoder.decode(json, "UTF-8");
          this.range = READER.readValue(decoded);
        }
        catch (IOException ee) {
          throw new WebApplicationException("Invalid Range", ee);
        }
      }
    }

    public RangeCondition getRange() {
      return range;
    }
  }
}
