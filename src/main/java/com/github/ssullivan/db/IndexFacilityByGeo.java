package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.index.RedisIndexFacilityByGeoPoint;
import com.google.inject.ImplementedBy;

@ImplementedBy(RedisIndexFacilityByGeoPoint.class)
public interface IndexFacilityByGeo extends IndexFacility {

}
