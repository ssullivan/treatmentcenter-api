package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IFindBySearchRequest;
import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchRequest;
import com.github.ssullivan.model.SearchResults;
import com.google.common.cache.LoadingCache;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PgFindBySearchRequest implements IFindBySearchRequest {
  private static final Logger LOGGER = LoggerFactory.getLogger(PgFindBySearchRequest.class);

  private final DSLContext dsl;
  private ObjectMapper objectMapper;
  private IFeedDao feedDao;
  private LoadingCache<String, Integer> serviceCodeToIntCache

  @Inject
  public PgFindBySearchRequest(final DSLContext dslContext, IFeedDao feedDao, final ObjectMapper objectMapper) {
    this.dsl = dslContext;
    this.objectMapper = objectMapper;
    this.feedDao = feedDao;
  }

  @Override
  public CompletionStage<SearchResults<Facility>> find(SearchRequest searchRequest, Page page)
      throws Exception {
    return this.feedDao
        .searchFeedId()
        .map(searchFeedId -> find(searchFeedId, searchRequest, page))
        .orElse(CompletableFuture.completedFuture(SearchResults.searchResults(0, new ArrayList<>())))
        .toCompletableFuture();

  }

  private CompletionStage<SearchResults<Facility>> find(final String feedId, SearchRequest searchRequest, Page page) {
    this.dsl.select(Tables.SERVICE.ID)
        .from(Tables.SERVICE)
        .
    this.dsl.select(Tables.LOCATION.JSON)
        .from(Tables.LOCATION)
        .where(Tables.LOCATION.SERVICES
    return CompletableFuture.completedFuture(SearchResults.searchResults(0, new ArrayList<>())));
  }


}
