package com.github.ssullivan.resources;

import com.github.ssullivan.auth.RequireApiKey;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;
import org.jooq.Require;

public class RecoveryHousingResource {

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @RequireApiKey
  public void addFormResponse() {

  }
}
