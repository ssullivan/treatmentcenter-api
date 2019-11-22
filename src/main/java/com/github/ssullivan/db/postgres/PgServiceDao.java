package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.ssullivan.db.IServiceCodesDao;
import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.model.Service;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgServiceDao implements IServiceCodesDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(PgCategoryDao.class);
    private final ObjectReader objectReader;
    private final ObjectWriter objectWriter;
    private DSLContext dsl;

    @Inject
    public PgServiceDao(final DSLContext dslContext, final ObjectMapper objectMapper) {
        this.dsl = dslContext;
        this.objectReader = objectMapper.readerFor(Service.class);
        this.objectWriter = objectMapper.writerFor(Service.class);
    }

    @Override
    public Service get(String id) throws IOException {
        final String json = this.dsl.select(Tables.SERVICE.JSON)
                .from(Tables.SERVICE)
                .fetchOne(Tables.SERVICE.JSON);

        return objectReader.readValue(json);
    }

    @Override
    public boolean delete(String id) throws IOException {
        return false;
    }

    @Override
    public Service getByServiceCode(String serviceCode) throws IOException {
        return get(serviceCode);
    }

    @Override
    public List<Service> listServices() throws IOException {
        return this.dsl
                .select(Tables.SERVICE.JSON)
                .from(Tables.SERVICE)
                .fetch(Tables.SERVICE.JSON)
                .stream()
                .map(json -> {
                    try {
                        return Optional.of(objectReader.readValue(json));
                    } catch (IOException e) {
                        LOGGER.error("Failed to deserialize JSON to category", e);
                    }
                    return Optional.empty();
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(it -> (Service) it)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> listServiceCodes() throws IOException {
        return this.dsl
                .selectDistinct(Tables.SERVICE.CODE)
                .fetch(Tables.SERVICE.CODE);
    }

    @Override
    public List<String> listServiceCodesInCategory(String category) throws IOException {
        return this.dsl
                .selectDistinct(Tables.SERVICE.CODE)
                .where(Tables.SERVICE.CATEGORY_CODE.eq(category))
                .fetch(Tables.SERVICE.CODE);

    }

    @Override
    public boolean addService(Service service) throws IOException {
        final String json = objectWriter.writeValueAsString(service);
        this.dsl.transaction(configuration -> {
            final DSLContext dsl = DSL.using(configuration);

            final Optional<Integer> pkOptional = dsl.select(Tables.SERVICE.ID)
                    .from(Tables.SERVICE)
                    .where(Tables.SERVICE.CODE.equalIgnoreCase(service.getCode()))
                    .fetchOptional(Tables.SERVICE.ID);

            if (pkOptional.isPresent()) {
                dsl.update(Tables.SERVICE)
                        .set(Tables.SERVICE.JSON, json)
                        .where(Tables.SERVICE.ID.eq(pkOptional.get()));
            }
            else {
                dsl.insertInto(Tables.SERVICE)
                        .set(Tables.SERVICE.CODE, service.getCode())
                        .set(Tables.SERVICE.CATEGORY_CODE, service.getCategoryCode())
                        .set(Tables.SERVICE.JSON, json)
                        .execute();
            }
        });

        return true;
    }
}
