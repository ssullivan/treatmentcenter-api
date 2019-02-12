package com.github.ssullivan.resources;

import com.github.ssullivan.db.redis.IRedisConnectionPool;
import io.lettuce.core.api.StatefulRedisConnection;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class ELBHealthCheckerResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(ELBHealthCheckerResource.class);

  private IRedisConnectionPool pool;

  public ELBHealthCheckerResource(final IRedisConnectionPool pool) {
    this.pool = pool;
  }

  @GET
  public Response pong() {
    try (StatefulRedisConnection<String, String> conn = pool.borrowConnection(500L)) {
      return Response.ok().entity("pong").build();
    } catch (Exception e) {
      LOGGER.error("Failed to get redis connection", e);
      return Response.serverError().build();
    }
  }
}
