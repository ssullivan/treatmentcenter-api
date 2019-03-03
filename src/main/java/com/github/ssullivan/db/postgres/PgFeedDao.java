package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.utils.ShortUuid;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgFeedDao implements IFeedDao {

  private static final Logger LOGGER = LoggerFactory.getLogger(PgFeedDao.class);

  private final DSLContext dsl;

  @Inject
  public PgFeedDao(final DSLContext dslContext) {
    this.dsl = dslContext;
  }

  @Override
  public Optional<String> nextFeedId() throws IOException {
    return Optional.ofNullable(ShortUuid.randomShortUuid());
  }

  @Override
  public Optional<String> setCurrentFeedId(String id) throws IOException {
    return setSearchFeedId(id);
  }

  @Override
  public Optional<String> setSearchFeedId(String id) throws IOException {
    this.dsl.insertInto(Tables.FEED_DETAIL)
        .set(Tables.FEED_DETAIL.IS_SEARCH_FEED, true)
        .set(Tables.FEED_DETAIL.ID, ShortUuid.decode(id))
        .onDuplicateKeyUpdate()
        .set(Tables.FEED_DETAIL.IS_SEARCH_FEED, true)
        .execute();
    return Optional.of("OK");
  }

  @Override
  public Collection<String> getFeedIds() throws IOException {
    return this.dsl.select(Tables.FEED_DETAIL.ID)
        .from(Tables.FEED_DETAIL)
        .fetch(Tables.FEED_DETAIL.ID)
        .stream()
        .map(ShortUuid::encode)
        .collect(Collectors.toSet());
  }

  @Override
  public void removeFeedId(String feedId) throws IOException {
    this.dsl.delete(Tables.FEED_DETAIL)
        .where(Tables.FEED_DETAIL.ID.eq(ShortUuid.decode(feedId)));
  }

  @Override
  public Optional<String> currentFeedId() throws IOException {
    return searchFeedId();
  }

  @Override
  public Optional<String> searchFeedId() throws IOException {
    return this.dsl.select(Tables.FEED_DETAIL.ID)
        .from(Tables.FEED_DETAIL)
        .where(Tables.FEED_DETAIL.IS_SEARCH_FEED.eq(true))
        .fetchOptional(Tables.FEED_DETAIL.ID)
        .map(ShortUuid::encode);
  }


}
