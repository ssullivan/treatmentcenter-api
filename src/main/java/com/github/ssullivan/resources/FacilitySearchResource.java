package com.github.ssullivan.resources;

import com.fasterxml.jackson.jaxrs.json.annotation.JSONP.Def;
import com.github.ssullivan.RequestUtils;
import com.github.ssullivan.api.IPostalcodeService;
import com.github.ssullivan.core.FacilityComparator;
import com.github.ssullivan.core.FacilitySearchService;
import com.github.ssullivan.core.analytics.CompositeFacilityScore;
import com.github.ssullivan.core.analytics.CompositeFacilityScore.Builder;
import com.github.ssullivan.core.analytics.Importance;
import com.github.ssullivan.core.analytics.TraumaTypes;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.FacilityWithRadius;
import com.github.ssullivan.model.GeoPoint;
import com.github.ssullivan.model.GeoRadiusCondition;
import com.github.ssullivan.model.MatchOperator;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchRequest;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.ServicesCondition;
import com.github.ssullivan.model.ServicesConditionFactory;
import com.github.ssullivan.model.SetOperation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.apiguardian.api.API;
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
      .compile("^!{0,1}[a-zA-Z0-9]{1,31}");
  public static final String TRAUMA_DOMESTIC_SEXUAL_NONE = "TRAUMA,DOMESTIC,SEXUAL,NONE";
  public static final String TRUE_FALSE = "true,false";
  public static final String VERY_SOMEWHAT_NOT = "VERY,SOMEWHAT,NOT";
  private final IFacilityDao facilityDao;
  private final IPostalcodeService postalcodeService;

  @Inject
  public FacilitySearchResource(final IFacilityDao facilityDao, final IPostalcodeService postalcodeService) {
    this.facilityDao = facilityDao;
    this.postalcodeService = postalcodeService;
  }


  @ApiOperation(value = "Find treatment facilities by their services and location. When multiple serviceCode, and matchAny sets are specified those results will be unified together",
      response = SearchResults.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200,
          message = "Search was completed successfully", response = SearchResults.class),
      @ApiResponse(code = 400, message = "Invalid query parameters"),
      @ApiResponse(code = 500,
          message = "An error occurred in the service while executing the search")
  })
  @GET
  @Path("/searchWithScore")
  @ManagedAsync
  public void findFacilitiesByServiceCodesV2WithScore(final @Suspended AsyncResponse asyncResponse,
      @ApiParam(value = "a U.S. PostalCode. If a (lat,lon) is specified that will take precedence", allowMultiple = false)
      @QueryParam("postalCode") final String postalCode,


      @ApiParam(value = "A comma separated list of service codes. service code prefixed with a single bang '!' will be negated", allowMultiple = true)
      @QueryParam("serviceCode") final List<String> serviceCodes,

      @ApiParam(value = "A comma separated list of service codes.", allowMultiple = true)
      @QueryParam("matchAny") final List<String> matchAnyServiceCodes,

      @ApiParam(value = "the latitude coordinate according to WGS84", allowableValues = "range[-90,90]")
      @QueryParam("lat") final Double lat,

      @ApiParam(value = "the longitude coordinate according to WGS84", allowableValues = "range[-180,180]")
      @QueryParam("lon") final Double lon,

      @ApiParam(value = "the radius distance") @DefaultValue("15")
      @QueryParam("distance") final Double distance,

      @ApiParam(value = "the unit of the radius distance. (meters, kilometers, feet, miles)", allowableValues = "m,km,ft,mi")
      @Pattern(regexp = "m|km|ft|mi", message = "Invalid distance unit")
      @DefaultValue("mi")
      @QueryParam("distanceUnit") final String distanceUnit,

      @ApiParam(value = "the number of results to skip", allowableValues = "range[0, 9999]")
      @Min(0) @Max(9999) @DefaultValue("0") @QueryParam("offset") final int offset,

      @ApiParam(value = "the number of results to return", allowableValues = "range[0, 9999]")
      @Min(0) @Max(9999) @DefaultValue("10") @QueryParam("size") final int size,

      @ApiParam(value="When multiple serviceCode, and matchAny sets are specified this controls how the final results are combined"
          , allowableValues = "AND,OR", defaultValue = "AND")
      @Pattern(regexp = "AND|OR", message = "Invalid boolean operator")
      @DefaultValue("AND")
      @QueryParam("operation")
      final String op,

      // Params for scoring

      @ApiParam(value = "The users date of birth in YYYY-MM-DD format [used for scoring]", example = "1980-01-16", allowEmptyValue = true)
      @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Invalid date of birth")
      @QueryParam("dob")
      final String dateOfBirth,


      // Params for heading support


      @ApiParam(value = "How important it is that a facility provides hearing support services", allowableValues = VERY_SOMEWHAT_NOT, allowEmptyValue = true)
      @DefaultValue("NOT")
      @QueryParam("hearingSupportImp")
      final Importance hearingSupportImportance,

      // Params for Lang support


      @ApiParam(value = "How important it is that a facility provides language support services", allowableValues = VERY_SOMEWHAT_NOT, allowEmptyValue = true)
      @DefaultValue("NOT")
      @QueryParam("langSupportImp")
      final Importance langSupportImp,

      // Params for Med Assisted Treatment

      // Params for Mental Health


      // Params for Military Status,


      @ApiParam(value = "Indicates how important military support is", allowableValues = VERY_SOMEWHAT_NOT, allowEmptyValue = true)
      @DefaultValue("NOT")
      @QueryParam("militaryImp")
      final Importance militaryImp,

      // Params for Service Setting

      // Params for Smoking Cessation


      // Params for Smoking Policy


      // Params for Substance Detox Services


      // Params for Trauma Services

      @ApiParam(value = "Indicates type of trauma support needed/wanted", allowableValues = TRAUMA_DOMESTIC_SEXUAL_NONE, allowEmptyValue = true, allowMultiple = true)
      @DefaultValue("NONE")
      @QueryParam("trauma")
      final Set<TraumaTypes> traumaTypes) {

    try {

      if (postalCode != null && postalCode.length() > 10) {
        asyncResponse.resume(Response.status(400)
            .entity(ImmutableMap.of("message", "Invalid postal code: Too Long"))
            .build()
        );
      }

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

      final SearchRequest searchRequest = new SearchRequest();
      final ServicesConditionFactory factory = new ServicesConditionFactory();
      searchRequest.setFinalSetOperation(SetOperation.fromBooleanOp(op));
      searchRequest.setServiceConditions(factory.fromRequestParams(serviceCodes, matchAnyServiceCodes));

      // protect against nefarious users sending too many sets in
      if (searchRequest.getConditions().size() > 15) {
        asyncResponse.resume(Response.status(400)
            .entity(ImmutableMap.of("message", "too many search conditions."))
            .build());
        return;
      }

      final Optional<ServicesCondition> invalidCondition = searchRequest.getConditions().stream()
          .filter(it -> it.size() > 200)
          .findAny();

      if (invalidCondition.isPresent()) {
        asyncResponse.resume(Response.status(400)
            .entity(ImmutableMap.of("message", "too many search conditions."))
            .build());
        return;
      }

      final Builder scoreBuilder =  new Builder()
          .withDateOfBirth(null == dateOfBirth ? null : LocalDate.parse(dateOfBirth))
          .withHearingSupport(hearingSupportImportance)
          .withLangSupport(langSupportImp)
          .withMilitarySupport(militaryImp)
          .withTraumaSupport(traumaTypes);

      if (lat != null && lon != null && !GeoPoint.isValidLatLong(lat, lon) && postalCode == null) {
        asyncResponse.resume(
            Response.status(400)
                .entity(ImmutableMap.of("message", "Invalid lat, lon coordinate")));
      } else if (lat != null && lon != null) {
        searchRequest.setGeoRadiusCondition(new GeoRadiusCondition(GeoPoint.geoPoint(lat, lon), distance, distanceUnit));
      } else if (postalCode != null) {
        ImmutableList<GeoPoint> geoPoints = postalcodeService.fetchGeos(postalCode);
        if (geoPoints == null || geoPoints.size() <= 0) {
          LOGGER.error("Failed to find GeoPoints for PostCode", postalCode);
          asyncResponse.resume(Response.status(400)
              .entity(ImmutableMap.of("message", "Failed to Geo locate postal code"))
              .build()
          );
        }
        else {
          searchRequest.setGeoRadiusCondition(new GeoRadiusCondition(geoPoints.get(0), distance, distanceUnit));
        }
      }
      this.facilityDao.find(searchRequest, Page.page(offset, size))
          .whenComplete((result, error) -> {
            if (error != null) {
              LOGGER.error("Failed to find facilities with service codes`", error);
              asyncResponse.resume(Response.serverError().build());
            }
            else {
              applyScores(searchRequest, scoreBuilder, result);
              asyncResponse.resume(Response.ok(result).build());
            }
          });

    } catch (Exception e) {
      LOGGER.error("Failed to find facilities with service codes`", e);
      asyncResponse.resume(Response.serverError().build());

      if (e instanceof InterruptedException) {
        LOGGER.error("Interrupted while handling request", e);
        Thread.currentThread().interrupt();
      }
    }
  }



  @ApiOperation(value = "Find treatment facilities by their services and location. When multiple serviceCode, and matchAny sets are specified those results will be unified together",
      response = SearchResults.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200,
          message = "Search was completed successfully", response = SearchResults.class),
      @ApiResponse(code = 400, message = "Invalid query parameters"),
      @ApiResponse(code = 500,
          message = "An error occurred in the service while executing the search")
  })
  @GET
  @Path("/v2/search")
  @ManagedAsync
  public void findFacilitiesByServiceCodesV2(final @Suspended AsyncResponse asyncResponse,
      @ApiParam(value = "a U.S. PostalCode. If a (lat,lon) is specified that will take precedence", allowMultiple = false)
      @QueryParam("postalCode") final String postalCode,


      @ApiParam(value = "A comma separated list of service codes. service code prefixed with a single bang '!' will be negated", allowMultiple = true)
      @QueryParam("serviceCode") final List<String> serviceCodes,

      @ApiParam(value = "A comma separated list of service codes.", allowMultiple = true)
      @QueryParam("matchAny") final List<String> matchAnyServiceCodes,

      @ApiParam(value = "the latitude coordinate according to WGS84", allowableValues = "range[-90,90]")
      @QueryParam("lat") final Double lat,

      @ApiParam(value = "the longitude coordinate according to WGS84", allowableValues = "range[-180,180]")
      @QueryParam("lon") final Double lon,

      @ApiParam(value = "the radius distance") @DefaultValue("15")
      @QueryParam("distance") final Double distance,

      @ApiParam(value = "the unit of the radius distance. (meters, kilometers, feet, miles)", allowableValues = "m,km,ft,mi")
      @Pattern(regexp = "m|km|ft|mi", message = "Invalid distance unit")
      @DefaultValue("mi")
      @QueryParam("distanceUnit") final String distanceUnit,

      @ApiParam(value = "the number of results to skip", allowableValues = "range[0, 9999]")
      @Min(0) @Max(9999) @DefaultValue("0") @QueryParam("offset") final int offset,

      @ApiParam(value = "the number of results to return", allowableValues = "range[0, 9999]")
      @Min(0) @Max(9999) @DefaultValue("10") @QueryParam("size") final int size,

      @ApiParam(value="When multiple serviceCode, and matchAny sets are specified this controls how the final results are combined"
          , allowableValues = "AND,OR", defaultValue = "AND")
      @Pattern(regexp = "AND|OR", message = "Invalid boolean operator")
      @DefaultValue("AND")
      @QueryParam("operation")
      final String op) {

    try {

      if (postalCode != null && postalCode.length() > 10) {
        asyncResponse.resume(Response.status(400)
            .entity(ImmutableMap.of("message", "Invalid postal code: Too Long"))
            .build()
        );
      }

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

      final SearchRequest searchRequest = new SearchRequest();
      final ServicesConditionFactory factory = new ServicesConditionFactory();
      searchRequest.setFinalSetOperation(SetOperation.fromBooleanOp(op));
      searchRequest.setServiceConditions(factory.fromRequestParams(serviceCodes, matchAnyServiceCodes));

      // protect against nefarious users sending too many sets in
      if (searchRequest.getConditions().size() > 15) {
        asyncResponse.resume(Response.status(400)
        .entity(ImmutableMap.of("message", "too many search conditions."))
            .build());
        return;
      }

      final Optional<ServicesCondition> invalidCondition = searchRequest.getConditions().stream()
          .filter(it -> it.size() > 200)
          .findAny();

      if (invalidCondition.isPresent()) {
        asyncResponse.resume(Response.status(400)
            .entity(ImmutableMap.of("message", "too many search conditions."))
            .build());
        return;
      }

      if (lat != null && lon != null && !GeoPoint.isValidLatLong(lat, lon) && postalCode == null) {
        asyncResponse.resume(
            Response.status(400)
                .entity(ImmutableMap.of("message", "Invalid lat, lon coordinate")));
      } else if (lat != null && lon != null) {
        searchRequest.setGeoRadiusCondition(new GeoRadiusCondition(GeoPoint.geoPoint(lat, lon), distance, distanceUnit));
      } else if (postalCode != null) {
        ImmutableList<GeoPoint> geoPoints = postalcodeService.fetchGeos(postalCode);
        if (geoPoints == null || geoPoints.size() <= 0) {
          LOGGER.error("Failed to find GeoPoints for PostCode", postalCode);
          asyncResponse.resume(Response.status(400)
              .entity(ImmutableMap.of("message", "Failed to Geo locate postal code"))
              .build()
          );
        }
        else {
          searchRequest.setGeoRadiusCondition(new GeoRadiusCondition(geoPoints.get(0), distance, distanceUnit));
        }
      }
      this.facilityDao.find(searchRequest, Page.page(offset, size))
          .whenComplete((result, error) -> {
              if (error != null) {
                LOGGER.error("Failed to find facilities with service codes`", error);
                asyncResponse.resume(Response.serverError().build());
              }
              else {
                asyncResponse.resume(Response.ok(result).build());
              }
          });

    } catch (Exception e) {
      LOGGER.error("Failed to find facilities with service codes`", e);
      asyncResponse.resume(Response.serverError().build());

      if (e instanceof InterruptedException) {
        LOGGER.error("Interrupted while handling request", e);
        Thread.currentThread().interrupt();
      }
    }
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
      @ApiParam(value = "a U.S. PostalCode. If a (lat,lon) is specified that will take precedence", allowMultiple = false)
      @QueryParam("postalCode") final String postalCode,


      @ApiParam(value = "The SAMSHA service code. service code prefixed with a single bang '!' will be negated", allowMultiple = true)
      @QueryParam("serviceCode") final List<String> serviceCodes,

      @QueryParam("matchAny") @DefaultValue("false") final Boolean matchAny,

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
      @Min(0) @Max(9999) @DefaultValue("10") @QueryParam("size") final int size,

      @ApiParam(value="When multiple serviceCode, and matchAny sets are specified this controls how the final results are combined"
          , allowableValues = "AND,OR", defaultValue = "AND")
      @Pattern(regexp = "AND|OR")
      @DefaultValue("AND")
      @QueryParam("operation")
      final String op) {
    try {
      if (postalCode != null && postalCode.length() > 10) {
        asyncResponse.resume(Response.status(400)
          .entity(ImmutableMap.of("message", "Invalid postal code: Too Long"))
            .build()
        );
      }

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

      final List<String> mustNotServiceCodes =
          serviceCodes
              .stream()
              .filter(it -> it.startsWith("!"))
              .map(it -> it.substring(1))
              .collect(Collectors.toList());

      final List<String> mustServiceCodes =
          serviceCodes
              .stream()
              .filter(it -> !it.startsWith("!"))
              .collect(Collectors.toList());

      if (mustNotServiceCodes.size() > 200 || mustServiceCodes.size() > 200) {
        asyncResponse.resume(Response.status(400)
            .entity(ImmutableMap.of("message", "too many service codes."))
            .build());
        return;
      }

      if (lat != null && lon != null && !GeoPoint.isValidLatLong(lat, lon) && postalCode == null) {
        asyncResponse.resume(
            Response.status(400)
                .entity(ImmutableMap.of("message", "Invalid lat, lon coordinate")));
      } else if (lat != null && lon != null) {
        asyncResponse.resume(this.facilityDao
            .findByServiceCodesWithin(serviceCodes, lon, lat, distance, distanceUnit,
                Page.page(offset, size)));
      } else if (postalCode != null) {
        ImmutableList<GeoPoint> geoPoints = postalcodeService.fetchGeos(postalCode);
        if (geoPoints == null || geoPoints.size() <= 0) {
          LOGGER.error("Failed to find GeoPoints for PostCode", postalCode);
          asyncResponse.resume(Response.status(400)
              .entity(ImmutableMap.of("message", "Failed to Geo locate postal code"))
              .build()
          );
        }

        SearchResults<FacilityWithRadius> results = this.facilityDao
            .findByServiceCodesWithin(mustServiceCodes, mustNotServiceCodes,
                matchAny,
                geoPoints.get(0).lon(),
                geoPoints.get(0).lat(),
                distance,
                distanceUnit,
                Page.page(offset, size));

        asyncResponse.resume(results);
      }
      else {
        final SearchResults<Facility> results = this.facilityDao.findByServiceCodes(mustServiceCodes, mustNotServiceCodes,
            matchAny, Page.page(offset, size));
        asyncResponse
            .resume(results);
      }
    } catch (IOException e) {
      LOGGER.error("Failed to find facilities with service codes`", e);
      asyncResponse.resume(Response.serverError().build());
    }
  }

  private static <F extends Facility> SearchResults<F> applyScores(final SearchRequest searchRequest,
      final CompositeFacilityScore.Builder builder, final SearchResults<F> searchResults) {
    applyScores(searchRequest.allServiceCodes(), builder, searchResults);
    return searchResults;
  }

  private static <F extends Facility> SearchResults<F> applyScores(final Set<String> serviceCodes,
      final CompositeFacilityScore.Builder builder, final SearchResults<F> searchResults) {
    final CompositeFacilityScore score = builder.withServiceCodes(serviceCodes).build();

    searchResults.hits().forEach(facility -> facility.setScore(score.score(facility)));

    return SearchResults.searchResults(searchResults.totalHits(),
        ImmutableList.sortedCopyOf(new FacilityComparator<>(), searchResults.hits()));
  }

}
