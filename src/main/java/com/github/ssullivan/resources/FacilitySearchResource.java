package com.github.ssullivan.resources;

import com.github.ssullivan.api.IPostalcodeService;
import com.github.ssullivan.core.FacilityComparator;
import com.github.ssullivan.core.analytics.CompositeFacilityScore;
import com.github.ssullivan.core.analytics.CompositeFacilityScore.Builder;
import com.github.ssullivan.core.analytics.Importance;
import com.github.ssullivan.core.analytics.TraumaTypes;
import com.github.ssullivan.db.IFindBySearchRequest;
import com.github.ssullivan.db.redis.search.FindBySearchRequest;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.GeoPoint;
import com.github.ssullivan.model.GeoRadiusCondition;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchRequest;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.ServicesCondition;
import com.github.ssullivan.model.ServicesConditionFactory;
import com.github.ssullivan.model.SetOperation;
import com.github.ssullivan.model.SortDirection;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

  public static final String TRAUMA_DOMESTIC_SEXUAL_NONE = "TRAUMA,DOMESTIC,SEXUAL,NONE";
  public static final String TRUE_FALSE = "true,false";
  public static final String VERY_SOMEWHAT_NOT = "VERY,SOMEWHAT,NOT";
  private static final Logger LOGGER = LoggerFactory.getLogger(FacilitySearchResource.class);
  private static final java.util.regex.Pattern RE_VALID_SAMSHA_SERVICE_CODE = java.util.regex.Pattern
      .compile("^!{0,1}[a-zA-Z0-9]{1,31}");
  private final IFindBySearchRequest facilitySearch;
  private final IPostalcodeService postalcodeService;

  @Inject
  public FacilitySearchResource(final IFindBySearchRequest facilitySearch,
      final IPostalcodeService postalcodeService) {
    this.facilitySearch = facilitySearch;
    this.postalcodeService = postalcodeService;
  }

  public static <F extends Facility> SearchResults<F> sort(final SearchResults<F> searchResults,
      final String sortField, final SortDirection sortDirection) {

    return SearchResults.searchResults(searchResults.totalHits(),
        ImmutableList.sortedCopyOf(new FacilityComparator<>(sortField, sortDirection),
            searchResults.hits()));
  }

  public static <F extends Facility> SearchResults<F> applyScores(final SearchRequest searchRequest,
      final CompositeFacilityScore.Builder builder, final SearchResults<F> searchResults) {
    applyScores(searchRequest.allServiceCodes(),
        builder, searchResults);
    return searchResults;
  }

  public static <F extends Facility> SearchResults<F> applyScores(final Set<String> serviceCodes,
      final CompositeFacilityScore.Builder builder, final SearchResults<F> searchResults) {
    final CompositeFacilityScore score = builder.withServiceCodes(serviceCodes).build();

    if (searchResults == null) {
      return SearchResults.empty();
    } else {
      searchResults.hits().forEach(facility -> {
        final double theScore = score.score(facility);

        facility.setScore(theScore);
      });
    }

    return searchResults;
  }

  @VisibleForTesting
  private Optional<GeoRadiusCondition> toGeoRadiusCondition(final Double lat, final Double lon,
      final Double distance,
      final String distanceUnit,
      final String postalCode) throws IllegalArgumentException {


    if (distance == null || distanceUnit == null || distanceUnit.isEmpty()) {
      return Optional.empty();
    }

    if (lat != null && lon != null && GeoPoint.isValidLatLong(lat, lon)) {
      return Optional.of(new GeoRadiusCondition(GeoPoint.geoPoint(lat, lon), distance, distanceUnit));
    }
    else if (postalCode != null && !postalCode.isEmpty()) {
      ImmutableList<GeoPoint> geoPoints = postalcodeService.fetchGeos(postalCode);
      if (geoPoints == null || geoPoints.size() <= 0) {
        LOGGER.error("Failed to find GeoPoints for PostCode", postalCode);
        return Optional.empty();
      } else {
        return Optional.of(new GeoRadiusCondition(geoPoints.get(0), distance, distanceUnit));
      }
    }
    else {
      return Optional.empty();
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
  @Path("/searchWithScore")
  @ManagedAsync
  public void findFacilitiesByServiceCodesV2WithScore(final @Suspended AsyncResponse asyncResponse,
      @ApiParam(value = "a U.S. PostalCode. If a (lat,lon) is specified that will take precedence",
          allowMultiple = false)
      @QueryParam("postalCode") final String postalCode,

      @ApiParam(value = "A comma separated list of service codes. "
          + "service code prefixed with a single bang '!' will be negated", allowMultiple = true)
      @QueryParam("serviceCode") final List<String> serviceCodes,

      @ApiParam(value = "A comma separated list of service codes.", allowMultiple = true)
      @QueryParam("matchAny") final List<String> matchAnyServiceCodes,

      @ApiParam(value = "the latitude coordinate according to WGS84", allowableValues = "range[-90,90]")
      @QueryParam("lat") final Double lat,

      @ApiParam(value = "the longitude coordinate according to WGS84", allowableValues = "range[-180,180]")
      @QueryParam("lon") final Double lon,

      @ApiParam(value = "the radius distance") @DefaultValue("15")
      @QueryParam("distance") final Double distance,

      @ApiParam(value = "the unit of the radius distance. (meters, kilometers, feet, miles)",
          allowableValues = "m,km,ft,mi")
      @Pattern(regexp = "m|km|ft|mi", message = "Invalid distance unit")
      @DefaultValue("mi")
      @QueryParam("distanceUnit") final String distanceUnit,

      @ApiParam(value = "the number of results to skip", allowableValues = "range[0, 9999]")
      @Min(0) @Max(9999) @DefaultValue("0") @QueryParam("offset") final int offset,

      @ApiParam(value = "the number of results to return", allowableValues = "range[0, 9999]")
      @Min(0) @Max(9999) @DefaultValue("10") @QueryParam("size") final int size,

      @ApiParam(value = "When multiple serviceCode, and matchAny sets are specified this controls "
          + "how the final results are combined"
          , allowableValues = "AND,OR", defaultValue = "AND")
      @Pattern(regexp = "AND|OR", message = "Invalid boolean operator")
      @DefaultValue("AND")
      @QueryParam("operation") final String op,

      // Params for scoring

      @ApiParam(value = "The users date of birth in YYYY-MM-DD format [used for scoring]",
          example = "1980-01-16", allowEmptyValue = true)
      @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Invalid date of birth")
      @QueryParam("dob") final String dateOfBirth,

      @ApiParam(value = "How important it is that a facility provides hearing support services",
          allowableValues = VERY_SOMEWHAT_NOT, allowEmptyValue = true)
      @DefaultValue("NOT")
      @QueryParam("hearingSupportImp") final Importance hearingSupportImportance,

      @ApiParam(value = "How important it is that a facility provides language support services",
          allowableValues = VERY_SOMEWHAT_NOT, allowEmptyValue = true)
      @DefaultValue("NOT")
      @QueryParam("langSupportImp") final Importance langSupportImp,

      @ApiParam(value = "Indicates how important military support is",
          allowableValues = VERY_SOMEWHAT_NOT, allowEmptyValue = true)
      @DefaultValue("NOT")
      @QueryParam("militaryImp") final Importance militaryImp,

      @ApiParam(value = "Indicates how import military family support is",
          allowableValues = VERY_SOMEWHAT_NOT, allowEmptyValue = true)
      @DefaultValue("NOT")
      @QueryParam("militaryFamilyImp") final Importance militaryFamilyImp,

      @ApiParam(value = "Indicates how important smoking cessation support is",
          allowableValues = VERY_SOMEWHAT_NOT, allowEmptyValue = true)
      @DefaultValue("NOT")
      @QueryParam("smokingCessationImp") final Importance smokingCessationImp,

      @ApiParam(value = "Indicates type of trauma support needed/wanted",
          allowableValues = TRAUMA_DOMESTIC_SEXUAL_NONE, allowEmptyValue = true, allowMultiple = true)
      @DefaultValue("NONE")
      @QueryParam("trauma") final Set<TraumaTypes> traumaTypes,

      @ApiParam(value = "Indicates the field to sort by. This only sorts the current results being returned")
      @DefaultValue("score")
      @QueryParam("sort") final String sortFields,

      @ApiParam(value = "Indicates the direction of the sort", allowableValues = "ASC,DESC")
      @DefaultValue("DESC")
      @QueryParam("sortDir") final SortDirection sortDirection) {

    try {

      if (postalCode != null && postalCode.length() > 10) {
        asyncResponse.resume(Response.status(400)
            .entity(ImmutableMap.of("message", "Invalid postal code: Too Long"))
            .build()
        );
      }


      final SearchRequest searchRequest = new SearchRequest();
      searchRequest.setSortDirection(sortDirection);
      searchRequest.setSortField(sortFields);

      final ServicesConditionFactory factory = new ServicesConditionFactory();
      searchRequest.setFinalSetOperation(SetOperation.fromBooleanOp(op));
      searchRequest
          .setServiceConditions(factory.fromRequestParams(serviceCodes, matchAnyServiceCodes));

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

      Builder scoreBuilder = new Builder()
          .withServiceCodes(ServicesConditionFactory.serviceCodes(searchRequest.getConditions()))
          .withDateOfBirth(null == dateOfBirth ? null : LocalDate.parse(dateOfBirth))
          .withHearingSupport(hearingSupportImportance)
          .withLangSupport(langSupportImp)
          .withMilitaryStatusSupport(militaryImp)
          .withMilitaryFamilySupport(militaryFamilyImp)
          .withSmokingCessationImportance(smokingCessationImp)
          .withTraumaSupport(traumaTypes);

      toGeoRadiusCondition(lat, lon, distance, distanceUnit, postalCode)
          .ifPresent(searchRequest::setGeoRadiusCondition);

      searchRequest.setCompositeFacilityScore(scoreBuilder.build());

      this.facilitySearch.find(searchRequest, Page.page(offset, size))
          .whenComplete((result, error) -> {
            if (error != null) {
              LOGGER.error("Failed to find facilities with service codes`", error);
              asyncResponse.resume(Response.serverError().build());
            } else {
              asyncResponse.resume(Response
                  .ok(applyScores(searchRequest, scoreBuilder, result)).build());
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
      @ApiParam(value = "a U.S. PostalCode. If a (lat,lon) is specified that will take precedence",
          allowMultiple = false)
      @QueryParam("postalCode") final String postalCode,

      @ApiParam(value = "A comma separated list of service codes. "
          + "service code prefixed with a single bang '!' will be negated", allowMultiple = true)
      @QueryParam("serviceCode") final List<String> serviceCodes,

      @ApiParam(value = "A comma separated list of service codes.", allowMultiple = true)
      @QueryParam("matchAny") final List<String> matchAnyServiceCodes,

      @ApiParam(value = "the latitude coordinate according to WGS84", allowableValues = "range[-90,90]")
      @QueryParam("lat") final Double lat,

      @ApiParam(value = "the longitude coordinate according to WGS84", allowableValues = "range[-180,180]")
      @QueryParam("lon") final Double lon,

      @ApiParam(value = "the radius distance") @DefaultValue("15")
      @QueryParam("distance") final Double distance,

      @ApiParam(value = "the unit of the radius distance. (meters, kilometers, feet, miles)",
          allowableValues = "m,km,ft,mi")
      @Pattern(regexp = "m|km|ft|mi", message = "Invalid distance unit")
      @DefaultValue("mi")
      @QueryParam("distanceUnit") final String distanceUnit,

      @ApiParam(value = "the number of results to skip", allowableValues = "range[0, 9999]")
      @Min(0) @Max(9999) @DefaultValue("0") @QueryParam("offset") final int offset,

      @ApiParam(value = "the number of results to return", allowableValues = "range[0, 9999]")
      @Min(0) @Max(9999) @DefaultValue("10") @QueryParam("size") final int size,

      @ApiParam(value = "When multiple serviceCode, and matchAny sets are "
          + "specified this controls how the final results are combined"
          , allowableValues = "AND,OR", defaultValue = "AND")
      @Pattern(regexp = "AND|OR", message = "Invalid boolean operator")
      @DefaultValue("AND")
      @QueryParam("operation") final String op,

      @ApiParam(value = "Indicates the field to sort by. "
          + "This only sorts the current results being returned")
      @DefaultValue("score")
      @QueryParam("sort") final String sortField,

      @ApiParam(value = "Indicates the direction of the sort", allowableValues = "ASC,DESC")
      @DefaultValue("DESC")
      @QueryParam("sortDir") final SortDirection sortDirection) {

    try {

      if (postalCode != null && postalCode.length() > 10) {
        asyncResponse.resume(Response.status(400)
            .entity(ImmutableMap.of("message", "Invalid postal code: Too Long"))
            .build()
        );
      }

      final SearchRequest searchRequest = new SearchRequest();
      searchRequest.setSortField(sortField);
      searchRequest.setSortDirection(sortDirection);

      final ServicesConditionFactory factory = new ServicesConditionFactory();
      searchRequest.setFinalSetOperation(SetOperation.fromBooleanOp(op));
      searchRequest
          .setServiceConditions(factory.fromRequestParams(serviceCodes, matchAnyServiceCodes));

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

      toGeoRadiusCondition(lat, lon, distance, distanceUnit, postalCode)
          .ifPresent(searchRequest::setGeoRadiusCondition);

      this.facilitySearch.find(searchRequest, Page.page(offset, size))
          .whenComplete((result, error) -> {
            if (error != null) {
              LOGGER.error("Failed to find facilities with service codes`", error);
              asyncResponse.resume(Response.serverError().build());
            } else {
              asyncResponse.resume(Response
                  .ok(applyScores(searchRequest, new CompositeFacilityScore.Builder(), result)).build());
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
      @ApiParam(value = "a U.S. PostalCode. If a (lat,lon) is specified that will take precedence",
          allowMultiple = false)
      @QueryParam("postalCode") final String postalCode,

      @ApiParam(value = "The SAMSHA service code. service code prefixed with a single bang '!'"
          + " will be negated", allowMultiple = true)
      @QueryParam("serviceCode") final List<String> serviceCodes,

      @QueryParam("matchAny") @DefaultValue("false") final Boolean matchAny,

      @ApiParam(value = "the latitude coordinate according to WGS84", allowableValues = "range[-90,90]")
      @QueryParam("lat") final Double lat,

      @ApiParam(value = "the longitude coordinate according to WGS84", allowableValues = "range[-180,180]")
      @QueryParam("lon") final Double lon,

      @ApiParam(value = "the radius distance") @DefaultValue("15")
      @QueryParam("distance") final Double distance,

      @ApiParam(value = "the unit of the radius distance. (meters, kilometers, feet, miles)",
          allowableValues = "m,km,ft,mi")
      @Pattern(regexp = "m|km|ft|mi")
      @DefaultValue("mi")
      @QueryParam("distanceUnit") final String distanceUnit,

      @ApiParam(value = "the number of results to skip", allowableValues = "range[0, 9999]")
      @Min(0) @Max(9999) @DefaultValue("0") @QueryParam("offset") final int offset,

      @ApiParam(value = "the number of results to return", allowableValues = "range[0, 9999]")
      @Min(0) @Max(9999) @DefaultValue("10") @QueryParam("size") final int size,

      @ApiParam(value = "When multiple serviceCode, and matchAny sets are specified this "
          + "controls how the final results are combined"
          , allowableValues = "AND,OR", defaultValue = "AND")
      @Pattern(regexp = "AND|OR")
      @DefaultValue("AND")
      @QueryParam("operation") final String op,

      @ApiParam(value = "Indicates the field to sort by. This only sorts the current results "
          + "being returned", allowEmptyValue = true, allowableValues = "score,radius,name1,name2,city,zip")
      @DefaultValue("score")
      @QueryParam("sort") final String sortFields,

      @ApiParam(value = "Indicates the direction of the sort", allowableValues = "ASC,DESC")
      @DefaultValue("DESC")
      @QueryParam("sortDir") final SortDirection sortDirection) {

    findFacilitiesByServiceCodesV2(asyncResponse, postalCode,
        serviceCodes,
        ImmutableList.of(),
        lat,
        lon,
        distance,
        distanceUnit,
        offset,
        size,
        op,
        sortFields,
        sortDirection
    );
  }

}
