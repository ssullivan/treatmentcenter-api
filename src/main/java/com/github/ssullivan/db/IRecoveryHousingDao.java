package com.github.ssullivan.db;

import com.github.ssullivan.db.postgres.RecoveryHousingDao;
import com.github.ssullivan.db.psql.tables.records.RecoveryHousingRecord;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.RecoveryHousingSearchRequest;
import com.google.common.collect.Range;
import com.google.inject.ImplementedBy;
import java.util.List;
import java.util.Map;

@ImplementedBy(RecoveryHousingDao.class)
public interface IRecoveryHousingDao {
  int upsert(final List<RecoveryHousingRecord> batch);

  int delete(final String feedName, Range<Long> feedRecordIdRange);

  int deleteByVersion(final String feedName, Range<Long> versionRange);

  default long count(Map<String, String> params) {
    return 0;
  }

  List<RecoveryHousingRecord> listAll(RecoveryHousingSearchRequest searchRequest, Page page);
}
