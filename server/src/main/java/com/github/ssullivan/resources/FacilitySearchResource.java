package com.github.ssullivan.resources;

import com.github.ssullivan.api.ISearchService;
import com.github.ssullivan.core.FacilitySearchService;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.model.FacilitySearchQuery;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
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
//
//  public void listFacilities() {
//    facilityDao.findByServiceCodes()
//  }


//  /**
//   * Find facilities that have the matching service codes.
//   *
//   * @param asyncResponse an instance of {@link AsyncResponse}
//   * @param facilitySearchQuery the query to search by
//   * @param offset how many documents to skip
//   * @param size how many documents to return
//   */
//  @POST
//  @ManagedAsync
//  public void find(@Suspended final AsyncResponse asyncResponse,
//      final FacilitySearchQuery facilitySearchQuery,
//      @Min(0) @Max(9999) @DefaultValue("0") @QueryParam("offset") final int offset,
//      @Min(0) @Max(9999) @DefaultValue("25") @QueryParam("size") final int size) {
//    try {
//      asyncResponse.resume(this.searchService.find(facilitySearchQuery, Page.page(offset, size)));
//    } catch (IOException e) {
//      LOGGER.error("Failed to find facilities because an I/O error occurred", e);
//      asyncResponse.resume(Response.serverError()
//          .entity(SearchResults.searchResults(0, ImmutableList.of())));
//
//    }
//  }
}
