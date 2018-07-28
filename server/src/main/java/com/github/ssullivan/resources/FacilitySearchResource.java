package com.github.ssullivan.resources;

import com.github.ssullivan.core.FacilitySearchService;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.model.GeoPoint;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.util.List;
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

import io.swagger.annotations.ApiParam;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Api(tags = "search")
@Consumes(MediaType.APPLICATION_JSON)
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Path("facilities")
public class FacilitySearchResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(FacilitySearchService.class);

  private final IFacilityDao facilityDao;

  @Inject
  public FacilitySearchResource(final IFacilityDao facilityDao) {
    this.facilityDao = facilityDao;
  }


  @ApiOperation(value = "Find treatment facilities by their services and location", response = SearchResults.class)
  @GET
  @Path("/search")
  @ManagedAsync
  public void findFacilitiesByServiceCodes(final @Suspended AsyncResponse asyncResponse,
      @ApiParam(value = "the SAMSHA service code", allowMultiple = true) @QueryParam("serviceCode") final List<String> serviceCodes,
      @ApiParam(value = "the latitude coordinate according to WGS84") @QueryParam("lat") final Double lat,
      @ApiParam(value = "the longitude coordinate according to WGS84") @QueryParam("lon") final Double lon,
      @ApiParam(value = "the radius distance") @DefaultValue("15") @QueryParam("distance") final Double distance,
      @ApiParam(value = "the unit of the radius distance. (meters, kilometers, feet, miles)", allowableValues = "m,km,ft,mi")
                                             @Pattern (regexp = "m|km|ft|mi") @DefaultValue("mi") @QueryParam("distanceUnit") final String distanceUnit,
      @Min(0) @Max(9999) @DefaultValue("0") @QueryParam("offset") final int offset,
      @Min(0) @Max(9999) @DefaultValue("10") @QueryParam("size") final int size) {
    try {
      if (lat != null && lon != null && !GeoPoint.isValidLatLong(lat, lon)) {
          asyncResponse.resume(
              Response.status(400)
              .entity(ImmutableMap.of("message", "Invalid lat, lon coordinate")));
      }
      else if (lat != null && lon != null) {
        asyncResponse.resume(this.facilityDao.findByServiceCodesWithin(serviceCodes, lon, lat, distance, distanceUnit, Page.page(offset, size)));
      }
      else {
        asyncResponse.resume(this.facilityDao.findByServiceCodes(serviceCodes, Page.page(offset, size)));
      }
    } catch (IOException e) {
      LOGGER.error("Failed to find facilities with service codes", e);
      asyncResponse.resume(Response.serverError().build());
    }
  }



}
