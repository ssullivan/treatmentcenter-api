package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IFindBySearchRequest;
import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.model.*;
import com.github.ssullivan.utils.ShortUuid;
import com.google.common.base.Strings;
import com.google.common.cache.LoadingCache;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PgFindBySearchRequest implements IFindBySearchRequest {
  private static final Logger LOGGER = LoggerFactory.getLogger(PgFindBySearchRequest.class);

  private final DSLContext dsl;
  private ObjectMapper objectMapper;
  private IFeedDao feedDao;
  private IServiceConditionToSql toSql;

  @Inject
  public PgFindBySearchRequest(final DSLContext dslContext, IFeedDao feedDao,
                               final IServiceConditionToSql conditionToSql,
                               final ObjectMapper objectMapper) {
    this.dsl = dslContext;
    this.objectMapper = objectMapper;
    this.feedDao = feedDao;
    this.toSql = conditionToSql;
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
    final Condition condition = toSql.toCondition(Tables.LOCATION.SERVICES, searchRequest.getFinalSetOperation(), searchRequest.getConditions());

    final long totalHits = this.dsl.selectCount()
            .from(Tables.LOCATION)
            .where(Tables.LOCATION.FEED_ID.eq(ShortUuid.decode(feedId)).and(condition))
            .fetchOne()
            .value1();

    final List<Facility> facilities = this.dsl.select(Tables.LOCATION.JSON)
            .from(Tables.LOCATION)
            .where(Tables.LOCATION.FEED_ID.eq(ShortUuid.decode(feedId)).and(condition))
            .orderBy(Tables.LOCATION.ID)
            .limit(page.offset(), page.size())
            .fetch(Tables.LOCATION.JSON)
            .stream()
            .map(this::deserialize)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    final SearchResults<Facility> searchResults = SearchResults.searchResults(totalHits, facilities);
    return CompletableFuture.completedFuture(searchResults);
  }

  private Facility deserialize(final String json) {
    try {
      return objectMapper.readValue(json, Facility.class);
    } catch (IOException e) {
      LOGGER.info("Failed to deserialize JSON for {}", json, e);
    }
    return null;
  }


}
