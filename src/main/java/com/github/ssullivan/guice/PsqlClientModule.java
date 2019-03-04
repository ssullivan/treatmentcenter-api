package com.github.ssullivan.guice;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.rds.auth.GetIamAuthTokenRequest;
import com.amazonaws.services.rds.auth.RdsIamAuthTokenGenerator;
import com.codahale.metrics.health.SharedHealthCheckRegistries;
import com.github.ssullivan.DatabaseConfig;
import com.github.ssullivan.RdsConfig;
import com.github.ssullivan.db.*;
import com.github.ssullivan.db.postgres.*;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PsqlClientModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsqlClientModule.class);
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
        bind(IFindBySearchRequest.class).to(PgFindBySearchRequest.class);
        bind(IManageFeeds.class).to(PgFeedManager.class);
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
        hikariConfig.addDataSourceProperty("user", this.psqlConfig.getUsername());
        hikariConfig.addDataSourceProperty("password", this.psqlConfig.getPassword() == null ? "" : this.psqlConfig.getPassword());
        hikariConfig.addDataSourceProperty("databaseName", this.psqlConfig.getDatabaseName());

        if (psqlConfig instanceof RdsConfig && ((RdsConfig) psqlConfig).isIamAuth()) {
            hikariConfig.addDataSourceProperty("password", generateAuthToken((RdsConfig) psqlConfig));
        }

        hikariConfig.setPoolName("api-postgres-pool");
        return hikariConfig;
    }

    @Provides
    @Singleton
    HikariDataSource provideDataSource(final HikariConfig hikariConfig) {
        final HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);

        try {
            hikariDataSource.setHealthCheckRegistry(SharedHealthCheckRegistries.getDefault());
            hikariDataSource.setMetricRegistry(SharedHealthCheckRegistries.getDefault());
        } catch (IllegalStateException e) {
            LOGGER.error("Failed to setup health/metrics integration!", e);
        }

        return hikariDataSource;
    }

    static String generateAuthToken(RdsConfig rdsConfig) {
        return generateAuthToken(rdsConfig.getRegion(), rdsConfig.getHost(), rdsConfig.getPort(), rdsConfig.getUsername());
    }

    static String generateAuthToken(String region, String hostName, int port, String username) {

        final RdsIamAuthTokenGenerator generator = RdsIamAuthTokenGenerator.builder()
            .credentials(new DefaultAWSCredentialsProviderChain())
            .region(region)
            .build();

        final String authToken = generator.getAuthToken(
            GetIamAuthTokenRequest.builder()
                .hostname(hostName)
                .port(port)
                .userName(username)
                .build());

        return authToken;
    }
}
