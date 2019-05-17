package com.github.ssullivan.auth;

import com.github.ssullivan.db.IApiKeyDao;
import java.io.IOException;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

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
      containerRequestContext.abortWith(Response.status(403).build());
    }
  }
}
