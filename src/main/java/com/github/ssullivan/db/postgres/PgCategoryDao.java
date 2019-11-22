package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.model.Category;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgCategoryDao implements ICategoryCodesDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(PgCategoryDao.class);
    private final ObjectReader objectReader;
    private final ObjectWriter objectWriter;
    private DSLContext dsl;

    @Inject
    public PgCategoryDao(final DSLContext dslContext, final ObjectMapper objectMapper) {
        this.dsl = dslContext;
        this.objectReader = objectMapper.readerFor(Category.class);
        this.objectWriter = objectMapper.writerFor(Category.class);
    }

    @Override
    public Category get(final String id) throws IOException {
        final String json = this.dsl.select(Tables.CATEGORY.JSON)
                .from(Tables.CATEGORY)
                .fetchOne(Tables.CATEGORY.JSON);

        return objectReader.readValue(json);
    }

    @Override
    public boolean delete(String id) throws IOException {
        return false;
    }

    @Override
    public Category getByCategoryCode(String categoryCode) throws IOException {
        return get(categoryCode);
    }

    @Override
    public List<String> listCategoryCodes() throws IOException {
        return this.dsl
                .selectDistinct(Tables.CATEGORY.CODE)
                .fetch(Tables.CATEGORY.CODE);
    }

    @Override
    public List<Category> listCategories() throws IOException {
        return this.dsl
                .select(Tables.CATEGORY.JSON)
                .from(Tables.CATEGORY)
                .fetch(Tables.CATEGORY.JSON)
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
                .map(it -> (Category) it)
                .collect(Collectors.toList());
    }

    @Override
    public boolean addCategory(Category category) throws IOException {
        final String json = objectWriter.writeValueAsString(category);
        this.dsl.transaction(configuration -> {
            final DSLContext dsl = DSL.using(configuration);

            final Optional<Integer> pkOptional = dsl.select(Tables.CATEGORY.ID)
                    .from(Tables.CATEGORY)
                    .where(Tables.CATEGORY.CODE.equalIgnoreCase(category.getCode()))
                    .fetchOptional(Tables.CATEGORY.ID);

            if (pkOptional.isPresent()) {
                dsl.update(Tables.CATEGORY)
                .set(Tables.CATEGORY.JSON, json)
                .where(Tables.CATEGORY.ID.eq(pkOptional.get()));
            }
            else {
                dsl.insertInto(Tables.CATEGORY)
                        .set(Tables.CATEGORY.CODE, category.getCode())
                        .set(Tables.CATEGORY.JSON, json)
                        .execute();
            }
        });

        return true;
    }
}
