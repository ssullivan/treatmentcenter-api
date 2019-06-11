package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.db.IRecoveryHousingDao;
import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.db.psql.tables.records.RecoveryHousingRecord;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.RecoveryHousingSearchRequest;
import com.github.ssullivan.model.conditions.RangeCondition;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Operator;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecoveryHousingDao implements IRecoveryHousingDao {

  private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryHousingDao.class);

  private final DSLContext dsl;
  private ObjectMapper objectMapper;

  @Inject
  public RecoveryHousingDao(DSLContext dsl, ObjectMapper objectMapper) {
    this.dsl = dsl;
    this.objectMapper = objectMapper;
  }

  @Override
  public int upsert(final List<RecoveryHousingRecord> batch) {
    Objects.requireNonNull(batch, "batch must not be null");
    /**
     * It's faster to do multiple inserts in a single transaction. The reason that
     * I'm not doing one big multi row insert is because the jooq api doesn't (easily)
     * support the syntax for onDuplicateKeyUpdate() for that use case.
     */
    return dsl.transactionResult(configuration -> {
      final DSLContext innerDsl = DSL.using(configuration);
      int total = 0;
      for (RecoveryHousingRecord record : batch) {
        total += innerDsl.insertInto(Tables.RECOVERY_HOUSING)
            .set(record)
            .onDuplicateKeyUpdate()
            .set(record)
            .execute();
      }
      return total;
    });
  }

  @Override
  public int delete(final String feedName, Range<Long> feedRecordIdRange) {
    Objects.requireNonNull(feedName, "feedName must not be null");
    Objects.requireNonNull(feedRecordIdRange, "range must not be null");

    return dsl.deleteFrom(Tables.RECOVERY_HOUSING)
              .where(Tables.RECOVERY_HOUSING.FEED_NAME.eq(feedName)
                  .and(toLongRangeCondition(Tables.RECOVERY_HOUSING.FEED_RECORD_ID, feedRecordIdRange)))
        .execute();
  }

  @Override
  public int deleteByVersion(String feedName, Range<Long> versionRange) {
    Objects.requireNonNull(feedName, "feedName must not be null");
    Objects.requireNonNull(versionRange, "range must not be null");

    return dsl.deleteFrom(Tables.RECOVERY_HOUSING)
        .where(Tables.RECOVERY_HOUSING.FEED_NAME.eq(feedName))
        .and(toLongRangeCondition(Tables.RECOVERY_HOUSING.FEED_VERSION, versionRange))
        .execute();
  }

  @Override
  public long count(Map<String, String> params) {
    Objects.requireNonNull(params, "params must not be null");

    return dsl.selectCount()
        .from(Tables.RECOVERY_HOUSING)
        .where(toCondition(params))
        .fetchOne(0, Integer.class)
        .longValue();
  }

  @Override
  public List<RecoveryHousingRecord> listAll(RecoveryHousingSearchRequest searchRequest,
      Page page) {
    Objects.requireNonNull(searchRequest, "searchRequest must not be null");
    Objects.requireNonNull(page, "page must not be null");

    Condition condition = DSL.trueCondition();
    if (null != searchRequest.getCapacity()) {
      condition = condition.and(toIntRangeCondition(Tables.RECOVERY_HOUSING.CAPACITY, searchRequest.getCapacity().toRange()));
    }
    if (null != searchRequest.getCity()) {
      condition = condition.and(Tables.RECOVERY_HOUSING.CITY.equalIgnoreCase(searchRequest.getCity()));
    }
    if (null != searchRequest.getZipcode()) {
      condition = condition.and(Tables.RECOVERY_HOUSING.POSTALCODE.equalIgnoreCase(searchRequest.getZipcode()));
    }

    return dsl.selectFrom(Tables.RECOVERY_HOUSING)
        .where(condition)
        .orderBy(Tables.RECOVERY_HOUSING.FEED_RECORD_ID)
        .offset(page.offset())
        .limit(page.size())
        .fetch();
  }

  public List<RecoveryHousingRecord> listAll(Map<String, String> params, Page page) {
    Objects.requireNonNull(params, "params must not be null");
    Objects.requireNonNull(page, "page must not be null");

    return dsl.selectFrom(Tables.RECOVERY_HOUSING)
        .where(toCondition(params))
        .orderBy(Tables.RECOVERY_HOUSING.FEED_RECORD_ID)
        .offset(page.offset())
        .limit(page.size())
        .fetch();
  }



  private static Condition toCondition(Map<String, String> params) {
    List<Condition> conditions = new ArrayList<>(3);
    for (Map.Entry<String, String> entry : params.entrySet()) {
      if (Tables.RECOVERY_HOUSING.POSTALCODE.getName().equals(entry.getKey())) {
        conditions.add(Tables.RECOVERY_HOUSING.POSTALCODE.eq(entry.getValue()));
      }
      else if (Tables.RECOVERY_HOUSING.STATE.getName().equals(entry.getKey())) {
        conditions.add(Tables.RECOVERY_HOUSING.STATE.eq(entry.getValue()));
      }
      else if (Tables.RECOVERY_HOUSING.CITY.getName().equals(entry.getKey())) {
        conditions.add(Tables.RECOVERY_HOUSING.CITY.eq(entry.getValue()));
      }
      else if (Tables.RECOVERY_HOUSING.FEED_NAME.getName().equals(entry.getKey())) {
        conditions.add(Tables.RECOVERY_HOUSING.FEED_NAME.eq(entry.getValue()));
      }
      else if (Tables.RECOVERY_HOUSING.CAPACITY.getName().equals(entry.getKey())) {

      }
    }
    return DSL.condition(Operator.AND, conditions);
  }

  private static Condition toIntRangeCondition(Field<Integer> field, Range<Integer> range) {

    if (range.hasLowerBound() && !range.hasUpperBound()) {
      return toLowerBoundType(field, range.lowerBoundType(), range.lowerEndpoint());
    }
    else if (!range.hasLowerBound() && range.hasUpperBound()) {
      return toUpperBoundType(field, range.upperBoundType(), range.upperEndpoint());
    }
    else if (range.hasLowerBound()) {
      return toLowerBoundType(field, range.lowerBoundType(), range.lowerEndpoint())
          .and(toUpperBoundType(field, range.upperBoundType(), range.upperEndpoint()));
    }
    else {
      return DSL.condition(false);
    }
  }

  private static <T> Condition toLongRangeCondition(Field<Long> field, Range<Long> range) {

    if (range.hasLowerBound() && !range.hasUpperBound()) {
      return toLowerBoundType(field, range.lowerBoundType(), range.lowerEndpoint());
    }
    else if (!range.hasLowerBound() && range.hasUpperBound()) {
      return toUpperBoundType(field, range.upperBoundType(), range.upperEndpoint());
    }
    else if (range.hasLowerBound()) {
      return toLowerBoundType(field, range.lowerBoundType(), range.lowerEndpoint())
          .and(toUpperBoundType(field, range.upperBoundType(), range.upperEndpoint()));
    }
    else {
      return DSL.condition(false);
    }
  }


  private static <T> Condition toUpperBoundType(Field<Long> field, BoundType boundType, long endpoint) {
    if (BoundType.CLOSED.equals(boundType)) {
      return field.lessOrEqual(endpoint);
    }
    return field.lessThan(endpoint);
  }

  private static Condition toLowerBoundType(Field<Long> field, BoundType boundType, long endpoint) {
    if (BoundType.CLOSED.equals(boundType)) {
      return field.greaterOrEqual(endpoint);
    }
    return field.greaterThan(endpoint);
  }

  private static <T> Condition toUpperBoundType(Field<Integer> field, BoundType boundType, int endpoint) {
    if (BoundType.CLOSED.equals(boundType)) {
      return field.lessOrEqual(endpoint);
    }
    return field.lessThan(endpoint);
  }

  private static Condition toLowerBoundType(Field<Integer> field, BoundType boundType, int endpoint) {
    if (BoundType.CLOSED.equals(boundType)) {
      return field.greaterOrEqual(endpoint);
    }
    return field.greaterThan(endpoint);
  }

}
