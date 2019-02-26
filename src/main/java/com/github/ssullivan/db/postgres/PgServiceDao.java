package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.db.psql.tables.daos.CategoryDao;
import com.github.ssullivan.model.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PgServiceDao implements ICategoryCodesDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(PgServiceDao.class);
    private final CategoryDao categoryDao;
    private final ObjectReader objectReader;
    private final ObjectWriter objectWriter;

    public PgServiceDao(final IJooqDaoFactory factory, final ObjectMapper objectMapper) {
        this.objectReader = objectMapper.readerFor(Category.class);
        this.objectWriter = objectMapper.writerFor(Category.class);
        this.categoryDao = factory.categoryDao();
    }

    @Override
    public Category get(String id) throws IOException {
        final com.github.ssullivan.db.psql.tables.pojos.Category cat = this.categoryDao.fetchOneByCode(id);
        return objectReader.readValue(cat.getJson());
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
        return this.categoryDao.configuration().dsl()
                .selectDistinct(Tables.CATEGORY.CODE)
                .fetch(Tables.CATEGORY.CODE);
    }

    @Override
    public List<Category> listCategories() throws IOException {
        return this.categoryDao.configuration().dsl()
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
        com.github.ssullivan.db.psql.tables.pojos.Category cat = new com.github.ssullivan.db.psql.tables.pojos.Category(null, category.getCode(), json);
        this.categoryDao.insert(cat);

        return true;
    }
}
