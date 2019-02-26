package com.github.ssullivan.db.postgres;

import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.psql.tables.daos.CategoryDao;
import com.github.ssullivan.db.psql.tables.daos.LocationDao;
import com.github.ssullivan.db.psql.tables.daos.ServiceDao;
import com.github.ssullivan.model.Category;
import com.github.ssullivan.model.Facility;
import com.zaxxer.hikari.HikariDataSource;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public class PgFacilityDao implements IFacilityDao {
    private final HikariDataSource hikariDataSource;
    private LocationDao locationDao;
    private CategoryDao categoryDao;
    private ServiceDao serviceDao;

    @Inject
    public PgFacilityDao(final HikariDataSource hikariDataSource,
                         final IJooqDaoFactory jooqDaoFactory) {
        this.hikariDataSource = hikariDataSource;
        this.locationDao = jooqDaoFactory.locationDao();
        this.categoryDao = jooqDaoFactory.categoryDao();
        this.serviceDao = jooqDaoFactory.serviceDao();
    }

    @Override
    public void addFacility(String feedId, Facility facility) throws IOException {

    }

    @Override
    public void addFacility(String feedId, List<Facility> facility) throws IOException {

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
