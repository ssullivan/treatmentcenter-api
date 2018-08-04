package com.github.ssullivan.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/_ping")
@Produces(MediaType.APPLICATION_JSON)
public class Healthcheck {

  @GET
  public String pong() {
    return "pong";
  }
}
