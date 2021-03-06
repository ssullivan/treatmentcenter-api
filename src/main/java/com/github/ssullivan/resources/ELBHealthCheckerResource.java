package com.github.ssullivan.resources;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class ELBHealthCheckerResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(ELBHealthCheckerResource.class);

  @Inject
  public ELBHealthCheckerResource() {
  }

  @GET
  public Response pong() {
    return Response.ok().build();
  }
}
