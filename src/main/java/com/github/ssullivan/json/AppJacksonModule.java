package com.github.ssullivan.json;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.ssullivan.db.psql.tables.records.RecoveryHousingRecord;
import com.github.ssullivan.model.conditions.RangeCondition;

public class AppJacksonModule extends SimpleModule {

  public AppJacksonModule() {
    super();

    addDeserializer(RangeCondition.class, new RangeConditionDeserializer());
    addSerializer(RecoveryHousingRecord.class, new RecoveryHousingRecordSerializer());
  }
}
