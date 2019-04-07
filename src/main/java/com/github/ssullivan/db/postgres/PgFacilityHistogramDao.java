package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.db.IFacilityHistogramDao;
import com.github.ssullivan.db.IServiceCodesDao;
import com.github.ssullivan.db.psql.Tables;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgFacilityHistogramDao implements IFacilityHistogramDao {

  private static final Logger LOGGER = LoggerFactory.getLogger(PgFacilityDao.class);

  private final DSLContext dsl;
  private ObjectMapper objectMapper;
  private IServiceCodesDao serviceCodesDao;

  @Inject
  public PgFacilityHistogramDao(final DSLContext dslContext, final IServiceCodesDao serviceCodesDao, final ObjectMapper objectMapper) {
    this.dsl = dslContext;
    this.objectMapper = objectMapper;
    this.serviceCodesDao = serviceCodesDao;

  }

  @Override
  public Map<String, Integer> toServicesHistogram(String groupBy) throws IOException {
    final List<String> serviceCodes = this.serviceCodesDao.listServiceCodes();
    final Field<?> servicesArrayAggField = DSL.arrayAgg(DSL.field("S")).as(Tables.LOCATION.SERVICES);

//    this.dsl.select(Tables.LOCATION.STATE, servicesArrayAggField)
//        .from(
//            this.dsl.select(Tables.LOCATION.STATE, DSL.unnest(Tables.LOCATION.SERVICES).field("S"))
//            .from(Tables.LOCATION)
//        )
    return null;
  }
}
