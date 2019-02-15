package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.index.RedisIndexFacilityByGeoPoint;
import com.google.inject.ImplementedBy;
import com.google.inject.Singleton;

@ImplementedBy(RedisIndexFacilityByGeoPoint.class)
public interface IndexFacilityByGeo extends IndexFacility {

}
