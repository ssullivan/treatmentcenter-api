package com.github.ssullivan.guice;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.rds.auth.GetIamAuthTokenRequest;
import com.amazonaws.services.rds.auth.RdsIamAuthTokenGenerator;
import com.codahale.metrics.health.SharedHealthCheckRegistries;
import com.github.ssullivan.AppConfig;
import com.github.ssullivan.DatabaseConfig;
import com.github.ssullivan.RdsConfig;
import com.github.ssullivan.db.*;
import com.github.ssullivan.db.postgres.*;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.inject.Inject;

import io.dropwizard.lifecycle.Managed;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PsqlClientModule extends DropwizardAwareModule<AppConfig> {
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

        if (psqlConfig instanceof RdsConfig) {
            bind(RdsConfig.class).toInstance((RdsConfig) psqlConfig);
            bind(IAMRdsAuthTokenRefresh.class).in(Singleton.class);
            bind(IManagedProvider.class).to(RdsDropwizardManagedProvider.class).in(Singleton.class);
        }
    }

    @Provides
    @Inject
    DSLContext providesDSLContext(final HikariDataSource hikariDataSource) {
        return DSL.using(hikariDataSource, SQLDialect.POSTGRES_10);
    }

    @Singleton
    @Provides
    HikariConfig providesHikariConfig() {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        hikariConfig.setUsername(psqlConfig.getUsername());
        hikariConfig.setPassword(this.psqlConfig.getPassword() == null ? "" : this.psqlConfig.getPassword());
        hikariConfig.addDataSourceProperty("serverName", this.psqlConfig.getHost());
        hikariConfig.addDataSourceProperty("databaseName", this.psqlConfig.getDatabaseName());
        hikariConfig.addDataSourceProperty("portNumber", this.psqlConfig.getPort());

        boolean setPasswordFromRds = false;
        if (psqlConfig instanceof RdsConfig && ((RdsConfig) psqlConfig).isIamAuth()) {
            final RdsConfig rdsConfig = (RdsConfig) psqlConfig;

            int retries = 3;
            do {
                try {
                    LOGGER.info("Attempting to generate RDS auth token from IAM");
                    hikariConfig.setPassword(generateAuthToken(rdsConfig));
                    setPasswordFromRds = true;
                    break;
                } catch (SdkClientException e) {
                    LOGGER.error("Failed to generate RDS auth token from IAM", e);
                }
            } while (retries-- > 0);
        }

        if (!setPasswordFromRds) {
            hikariConfig.setPassword(this.psqlConfig.getPassword() == null ? "" : this.psqlConfig.getPassword());
        }

        hikariConfig.setPoolName("api-postgres-pool");
        return hikariConfig;
    }

    @Provides
    @Singleton
    HikariDataSource provideDataSource(final HikariConfig hikariConfig) {
        final HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);


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
