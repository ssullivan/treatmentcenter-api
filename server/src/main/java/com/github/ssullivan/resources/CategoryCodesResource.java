package com.github.ssullivan.resources;

import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.model.Category;
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

@Api(tags = "categories")
@Consumes(MediaType.APPLICATION_JSON)
@Produces({MediaType.APPLICATION_JSON})
@Path("categories")
public class CategoryCodesResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(CategoryCodesResource.class);

  private final ICategoryCodesDao categoryCodesDao;


  @Inject
  public CategoryCodesResource(ICategoryCodesDao categoryCodesDao) {
    this.categoryCodesDao = categoryCodesDao;
  }

  @ApiOperation(value = "List categories of treatments that a facility can provide",
      responseContainer = "List",
      response = Category.class
  )
  @GET
  @ManagedAsync
  public void listCategories(@Suspended AsyncResponse asyncResponse) {
    try {
      final List<Category> services = this.categoryCodesDao.listCategories();
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
