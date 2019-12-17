package com.github.ssullivan.resources;

import com.github.ssullivan.db.IServiceCodesDao;
import com.github.ssullivan.model.Service;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.ManagedAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Api(tags = "services")
@Consumes(MediaType.APPLICATION_JSON)
@Produces({MediaType.APPLICATION_JSON})
@Path("services")
public class ServiceCodesResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceCodesResource.class);
  private final IServiceCodesDao serviceCodesDao;

  @Inject
  public ServiceCodesResource(IServiceCodesDao serviceCodesDao) {
    this.serviceCodesDao = serviceCodesDao;
  }

  @ApiOperation(value = "List services that a treatment facility can provide.",
      responseContainer = "List",
      response = Service.class
  )
  @GET
  @ManagedAsync
  public void listServices(@Suspended AsyncResponse asyncResponse) {
    try {
      final List<Service> services = this.serviceCodesDao.listServices();
      final CacheControl cacheControl = new CacheControl();
      cacheControl.setMaxAge((int) TimeUnit.DAYS.toSeconds(1));

      final Response successResponse = Response.ok(services)
          .cacheControl(cacheControl)
          .build();

      asyncResponse.resume(successResponse);
    }
    catch (IOException e) {
      LOGGER.error("Failed to list services", e);
      asyncResponse.resume(Response.serverError().build());
    }
  }
}
