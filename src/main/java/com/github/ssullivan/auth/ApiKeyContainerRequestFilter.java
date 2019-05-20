package com.github.ssullivan.auth;

import com.github.ssullivan.db.IApiKeyDao;
import java.io.IOException;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import jdk.nashorn.internal.ir.annotations.Immutable;
import jersey.repackaged.com.google.common.collect.ImmutableMap;

@RequireApiKey
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyContainerRequestFilter implements ContainerRequestFilter {
  private static final String ApiKeyHeader = "API-Key";
  private IApiKeyDao apiKeyDao;

  @Inject
  public ApiKeyContainerRequestFilter(IApiKeyDao apiKeyDao) {
    this.apiKeyDao = apiKeyDao;
  }

  @Override
  public void filter(final ContainerRequestContext containerRequestContext) throws IOException {
    final String apiKey = containerRequestContext.getHeaderString(ApiKeyHeader);
    if (! apiKeyDao.isValidApiKey(apiKey)) {
      containerRequestContext.abortWith(Response.status(403).type(MediaType.APPLICATION_JSON)
          .entity(ImmutableMap.of("message", "A valid API-Key was not provided")).build());
    }
  }
}
