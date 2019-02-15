package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.index.RedisIndexFacilityByCategoryCode;
import com.google.inject.ImplementedBy;
import com.google.inject.Singleton;

@ImplementedBy(RedisIndexFacilityByCategoryCode.class)
public interface IndexFacilityByCategoryCode extends IndexFacility {

}
