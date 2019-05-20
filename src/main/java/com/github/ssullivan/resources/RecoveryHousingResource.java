package com.github.ssullivan.resources;

import com.github.ssullivan.auth.RequireApiKey;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jooq.Require;

@Path("/recovery")
public class RecoveryHousingResource {

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @RequireApiKey
  public Response addFormResponse() {
    return Response.ok().build();
  }

  @GET
  @Path("/hello")
  public Response noAuthKey() {
    return Response.ok().build();
  }
}
