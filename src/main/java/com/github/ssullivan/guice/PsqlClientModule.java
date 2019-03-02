package com.github.ssullivan.guice;

import com.codahale.metrics.health.SharedHealthCheckRegistries;
import com.github.ssullivan.DatabaseConfig;
import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IServiceCodesDao;
import com.github.ssullivan.db.IndexFacility;
import com.github.ssullivan.db.postgres.NoOpIndexFacility;
import com.github.ssullivan.db.postgres.PgCategoryDao;
import com.github.ssullivan.db.postgres.PgFacilityDao;
import com.github.ssullivan.db.postgres.PgFeedDao;
import com.github.ssullivan.db.postgres.PgServiceDao;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

public class PsqlClientModule extends AbstractModule {
    private final DatabaseConfig psqlConfig;

    public PsqlClientModule(final DatabaseConfig psqlConfig) {
        this.psqlConfig = psqlConfig;
    }

    @Override
    protected void configure() {
        bind(IFacilityDao.class).to(PgFacilityDao.class);
        bind(ICategoryCodesDao.class).to(PgCategoryDao.class);
        bind(IServiceCodesDao.class).to(PgServiceDao.class);
        bind(IndexFacility.class).to(NoOpIndexFacility.class);
        bind(IFeedDao.class).to(PgFeedDao.class);
    }

    @Provides
    @Inject
    DSLContext providesDSLContext(final HikariDataSource hikariDataSource) {
        return DSL.using(hikariDataSource, SQLDialect.POSTGRES_10);
    }

    @Provides
    HikariConfig providesHikariConfig() {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        hikariConfig.setUsername(psqlConfig.getUsername());
        hikariConfig.addDataSourceProperty("dataSource.user", this.psqlConfig.getUsername());
        hikariConfig.addDataSourceProperty("dataSource.password", this.psqlConfig.getPassword());
        hikariConfig.addDataSourceProperty("dataSource.databaseName", this.psqlConfig.getDatabaseName());
        hikariConfig.setPoolName("api-postgres-pool");
        return hikariConfig;
    }

    @Provides
    @Singleton
    HikariDataSource provideDataSource(final HikariConfig hikariConfig) {
        final HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
        hikariDataSource.setHealthCheckRegistry(SharedHealthCheckRegistries.getDefault());
        hikariDataSource.setMetricRegistry(SharedHealthCheckRegistries.getDefault());

        return hikariDataSource;
    }
}
