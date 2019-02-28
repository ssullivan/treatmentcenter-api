package com.github.ssullivan.guice;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.SharedHealthCheckRegistries;
import com.github.ssullivan.DatabaseConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.inject.Inject;

public class PsqlClientModule extends AbstractModule {
    private final DatabaseConfig psqlConfig;

    public PsqlClientModule(final DatabaseConfig psqlConfig) {
        this.psqlConfig = psqlConfig;
    }

    @Override
    protected void configure() {

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
