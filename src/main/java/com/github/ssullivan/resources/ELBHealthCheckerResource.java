package com.github.ssullivan.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class ELBHealthCheckerResource {

  @GET
  public String pong() {
    return "pong";
  }
}
