package com.github.ssullivan.resources;

import com.github.ssullivan.core.FacilitySearchService;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.model.GeoPoint;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Api(tags = "search")
@Consumes(MediaType.APPLICATION_JSON)
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Path("facilities")
public class FacilitySearchResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(FacilitySearchService.class);
  private static final java.util.regex.Pattern RE_VALID_SAMSHA_SERVICE_CODE = java.util.regex.Pattern
      .compile("^[A-Z0-9]{2,4}");
  private final IFacilityDao facilityDao;

  @Inject
  public FacilitySearchResource(final IFacilityDao facilityDao) {
    this.facilityDao = facilityDao;
  }


  @ApiOperation(value = "Find treatment facilities by their services and location",
      response = SearchResults.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200,
          message = "Search was completed successfully", response = SearchResults.class),
      @ApiResponse(code = 400, message = "Invalid query parameters"),
      @ApiResponse(code = 500,
          message = "An error occurred in the service while executing the search")
  })
  @GET
  @Path("/search")
  @ManagedAsync
  public void findFacilitiesByServiceCodes(final @Suspended AsyncResponse asyncResponse,
      @ApiParam(value = "the SAMSHA service code", allowMultiple = true)
      @QueryParam("serviceCode") final List<String> serviceCodes,

      @ApiParam(value = "the latitude coordinate according to WGS84", allowableValues = "range[-90,90]")
      @QueryParam("lat") final Double lat,

      @ApiParam(value = "the longitude coordinate according to WGS84", allowableValues = "range[-180,180]")
      @QueryParam("lon") final Double lon,

      @ApiParam(value = "the radius distance") @DefaultValue("15")
      @QueryParam("distance") final Double distance,

      @ApiParam(value = "the unit of the radius distance. (meters, kilometers, feet, miles)", allowableValues = "m,km,ft,mi")
      @Pattern(regexp = "m|km|ft|mi")
      @DefaultValue("mi")
      @QueryParam("distanceUnit") final String distanceUnit,

      @ApiParam(value = "the number of results to skip", allowableValues = "range[0, 9999]")
      @Min(0) @Max(9999) @DefaultValue("0") @QueryParam("offset") final int offset,

      @ApiParam(value = "the number of results to return", allowableValues = "range[0, 9999]")
      @Min(0) @Max(9999) @DefaultValue("10") @QueryParam("size") final int size) {
    try {
      // Validate that the service codes are valid
      final Optional<String> firstInvalidServiceCode = serviceCodes.stream()
          .filter(code -> !RE_VALID_SAMSHA_SERVICE_CODE.matcher(code).matches())
          .findFirst();

      // If any of the service codes are invalid return a 400
      if (firstInvalidServiceCode.isPresent()) {
        asyncResponse.resume(Response.status(400)
            .entity(ImmutableMap
                .of("message", "Invalid service code: " + firstInvalidServiceCode.get())).build());
        return;
      }

      if (lat != null && lon != null && !GeoPoint.isValidLatLong(lat, lon)) {
        asyncResponse.resume(
            Response.status(400)
                .entity(ImmutableMap.of("message", "Invalid lat, lon coordinate")));
      } else if (lat != null && lon != null) {
        asyncResponse.resume(this.facilityDao
            .findByServiceCodesWithin(serviceCodes, lon, lat, distance, distanceUnit,
                Page.page(offset, size)));
      } else {
        asyncResponse
            .resume(this.facilityDao.findByServiceCodes(serviceCodes, Page.page(offset, size)));
      }
    } catch (IOException e) {
      LOGGER.error("Failed to find facilities with service codes`", e);
      asyncResponse.resume(Response.serverError().build());
    }
  }


}
