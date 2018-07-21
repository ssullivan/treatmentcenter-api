package com.github.ssullivan.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.ssullivan.api.ICategoryAndServiceService;
import com.github.ssullivan.model.SearchResults;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import org.elasticsearch.common.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Consumes(MediaType.APPLICATION_JSON)
@Produces({MediaType.APPLICATION_JSON})
@Path("servicecodes")
public class ServiceCodesResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceCodesResource.class);

  private static final TypeReference<Map<String, Set<String>>> MAP_OF_SETS =
      new TypeReference<Map<String, Set<String>>>() {
      };

  private final ICategoryAndServiceService serviceCodesService;
  private final ObjectMapper objectMapper;
  private final ObjectReader mapSetReader;

  /**
   * Create a new instance of {@link ServiceCodesResource}.
   *
   * @param serviceCodesService an instance of {@link ServiceCodesResource}
   * @param objectMapper an instance of {@link ObjectMapper}
   */
  @Inject
  public ServiceCodesResource(final ICategoryAndServiceService serviceCodesService,
      ObjectMapper objectMapper) {
    this.serviceCodesService = serviceCodesService;
    this.objectMapper = objectMapper;
    this.mapSetReader = objectMapper.readerFor(MAP_OF_SETS);
  }


  /**
   * List all available service codes.
   *
   * @param asyncResponse an instance of {@link AsyncResponse}
   * @param filter a JSON encoded string of map[string][string[]]
   */
  @GET
  public void listServiceCodes(@Suspended final AsyncResponse asyncResponse,
      @QueryParam("filters") final String filter) {

    /**
     * filters should be a JSON encoded list of filters.
     * map[string][string[]]
     *
     */
    Map<String, Set<String>> filters = new HashMap<>();
    if (!Strings.isNullOrEmpty(filter)) {
      try {
        filters = this.mapSetReader.readValue(filter);
      } catch (IOException e) {
        LOGGER.error("Invalid filters specified. Must be JSON encoded map[string][string[]]", e);
        asyncResponse.resume(Response.status(400)
            .entity("Invalid filters specified. Must be JSON encoded map[string][string[]]")
            .build());
        return;
      }
    }

    // service codes don't really change much
    try {
      final CacheControl cacheControl = new CacheControl();
      cacheControl.setMaxAge(86400);

      final SearchResults searchResults = this.serviceCodesService.listServiceCodes();
      final String searchResultJson = this.objectMapper.writeValueAsString(searchResults);
      final EntityTag etag = new EntityTag(DigestUtils.sha256Hex(searchResultJson));

      final Response response = Response.ok(Entity.json(searchResultJson))
          .cacheControl(cacheControl)
          .tag(etag)
          .build();

      asyncResponse.resume(response);

    } catch (IOException e) {
      LOGGER.error("Failed to list service codes", e);
      asyncResponse.resume(Response.serverError().build());
    }
  }

}
