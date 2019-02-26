package com.github.ssullivan.db.postgres;

import com.github.ssullivan.db.psql.tables.FeedDetail;
import com.github.ssullivan.db.psql.tables.daos.CategoryDao;
import com.github.ssullivan.db.psql.tables.daos.FeedDetailDao;
import com.github.ssullivan.db.psql.tables.daos.LocationDao;
import com.github.ssullivan.db.psql.tables.daos.ServiceDao;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.inject.Inject;


public class JooqDaoFactory implements IJooqDaoFactory {
    private DSLContext dsl;

    @Inject
    public JooqDaoFactory(final HikariDataSource hikariDataSource) {
        this.dsl = DSL.using(hikariDataSource, SQLDialect.POSTGRES_10);
    }

    @Override
    public LocationDao locationDao() {
        final LocationDao locationDao = new LocationDao();
        locationDao.setConfiguration(dsl.configuration());
        return locationDao;
    }

    @Override
    public CategoryDao categoryDao() {
        final CategoryDao categoryDao = new CategoryDao();
        categoryDao.setConfiguration(dsl.configuration());
        return categoryDao;
    }

    @Override
    public ServiceDao serviceDao() {
        final ServiceDao serviceDao = new ServiceDao();
        serviceDao.setConfiguration(dsl.configuration());
        return serviceDao;
    }

    @Override
    public FeedDetailDao feedDetailDao() {
        final FeedDetailDao feedDetailDao = new FeedDetailDao();
        feedDetailDao.setConfiguration(dsl.configuration());
        return feedDetailDao;
    }
}
