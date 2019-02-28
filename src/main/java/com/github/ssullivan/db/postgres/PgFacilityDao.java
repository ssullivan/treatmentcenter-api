package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ssullivan.core.UncheckedJsonProcessingException;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.db.psql.tables.records.LocationRecord;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.utils.ShortUuid;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.postgis.Point;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class PgFacilityDao implements IFacilityDao {
    private final DSLContext dsl;
    private ObjectMapper objectMapper;

    @Inject
    public PgFacilityDao(final DSLContext dslContext, final ObjectMapper objectMapper) {
        this.dsl = dslContext;
        this.objectMapper = objectMapper;
    }

    private Map<String, Integer> createCategoryCodeLookupTable(final DSLContext dsl) {
        return dsl.selectDistinct(Tables.CATEGORY.CODE, Tables.CATEGORY.ID)
                .from(Tables.CATEGORY)
                .fetchMap(Tables.CATEGORY.CODE, Tables.CATEGORY.ID);
    }

    private Map<String, Integer> createServiceCodeLookupTable(final DSLContext dsl) {
        return dsl.selectDistinct(Tables.SERVICE.CODE, Tables.SERVICE.ID)
                .from(Tables.SERVICE)
                .fetchMap(Tables.SERVICE.CODE, Tables.SERVICE.ID);
    }

    private Integer[] convertToInts(final Map<String,Integer> lookup, final Collection<String> items) {
         return items.stream()
                .map(code -> lookup.getOrDefault(code, null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                .toArray(new Integer[]{});
    }

    @Override
    public void addFacility(String feedId, Facility facility) throws IOException {
        final String json = objectMapper.writeValueAsString(facility);

        dsl.transaction(configuration -> {
            final DSLContext innerDsl = DSL.using(configuration);
            final Map<String, Integer> categoryLookup = createCategoryCodeLookupTable(innerDsl);
            final Map<String, Integer> serviceLookup = createServiceCodeLookupTable(innerDsl);

            final Integer[] serviceCodeIds = convertToInts(serviceLookup, facility.getServiceCodes());
            final Integer[] categoryCodeIds = convertToInts(categoryLookup, facility.getCategoryCodes());

            innerDsl.insertInto(Tables.LOCATION)
                    .set(Tables.LOCATION.CATS, categoryCodeIds)
                    .set(Tables.LOCATION.SERVICES, serviceCodeIds)
                    .set(Tables.LOCATION.CITY, facility.getCity())
                    .set(Tables.LOCATION.COUNTY, facility.getCounty())
                    .set(Tables.LOCATION.FEED_ID, ShortUuid.decode(facility.getFeedId()))
                    .set(Tables.LOCATION.STATE, facility.getState())
                    .set(Tables.LOCATION.STREET, facility.getStreet())
                    .set(Tables.LOCATION.POSTALCODE, facility.getZip())
                    .set(Tables.LOCATION.LAT, facility.getLocation() != null ? facility.getLocation().lat() : null)
                    .set(Tables.LOCATION.LON, facility.getLocation() != null ? facility.getLocation().lon() : null)
                    .set(Tables.LOCATION.ID, ShortUuid.decode(facility.getId()))
                    .set(Tables.LOCATION.GEOG, facility.getLocation() != null ? new Point(facility.getLocation().lon(), facility.getLocation().lat(), 0) : null)
                    .execute();
        });
    }

    @Override
    public void addFacility(String feedId, List<Facility> batch) throws IOException {
        try {
            dsl.transaction(configuration -> {
                final DSLContext innerDsl = DSL.using(configuration);
                final Map<String, Integer> categoryLookup = createCategoryCodeLookupTable(innerDsl);
                final Map<String, Integer> serviceLookup = createServiceCodeLookupTable(innerDsl);


                final List<LocationRecord> records = batch.stream().map(facility -> {
                    try {
                        final String json = objectMapper.writeValueAsString(facility);

                        final Integer[] serviceCodeIds = convertToInts(serviceLookup, facility.getServiceCodes());
                        final Integer[] categoryCodeIds = convertToInts(categoryLookup, facility.getCategoryCodes());

                        LocationRecord locationRecord = new LocationRecord()
                                .setId(ShortUuid.decode(facility.getId()))
                                .setCats(categoryCodeIds)
                                .setServices(serviceCodeIds)
                                .setJson(json)
                                .setState(facility.getState())
                                .setCounty(facility.getCounty())
                                .setCity(facility.getCity())
                                .setStreet(facility.getStreet())
                                .setPostalcode(facility.getZip());

                        if (facility.getLocation() != null) {
                            locationRecord
                                    .setLat(facility.getLocation().lat())
                                    .setLon(facility.getLocation().lon());
                        }

                        return locationRecord;
                    }
                    catch (JsonProcessingException e) {
                        throw new UncheckedJsonProcessingException(e);
                    }
                }).collect(Collectors.toList());

                innerDsl.batchInsert(records);
            });
        }
        catch (UncheckedJsonProcessingException e) {
            throw new IOException("Failed to serialize to JSON", e);
        }
    }

    @Override
    public Facility getFacility(String id) throws IOException {
        return null;
    }

    @Override
    public List<Facility> fetchBatch(Collection<String> ids) {
        return null;
    }

    @Override
    public CompletionStage<List<Facility>> fetchBatchAsync(Collection<String> ids) {
        return null;
    }

    @Override
    public Set<String> getKeysForFeed(String feedId) throws IOException {
        return null;
    }

    @Override
    public Boolean expire(String id, long seconds) throws IOException {
        return null;
    }

    @Override
    public Boolean expire(String feed, long seconds, boolean overwrite) throws IOException {
        return null;
    }
}
