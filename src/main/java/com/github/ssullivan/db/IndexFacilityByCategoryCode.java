package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.index.RedisIndexFacilityByCategoryCode;
import com.google.inject.ImplementedBy;

@ImplementedBy(RedisIndexFacilityByCategoryCode.class)
public interface IndexFacilityByCategoryCode extends IndexFacility {

}
