package com.github.ssullivan.guice;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.rds.auth.GetIamAuthTokenRequest;
import com.amazonaws.services.rds.auth.RdsIamAuthTokenGenerator;
import com.github.ssullivan.AppConfig;
import com.github.ssullivan.DatabaseConfig;
import com.github.ssullivan.RdsConfig;
import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IFindBySearchRequest;
import com.github.ssullivan.db.IManageFeeds;
import com.github.ssullivan.db.IServiceCodesDao;
import com.github.ssullivan.db.IndexFacility;
import com.github.ssullivan.db.postgres.NoOpIndexFacility;
import com.github.ssullivan.db.postgres.PgCategoryDao;
import com.github.ssullivan.db.postgres.PgFacilityDao;
import com.github.ssullivan.db.postgres.PgFeedDao;
import com.github.ssullivan.db.postgres.PgFeedManager;
import com.github.ssullivan.db.postgres.PgFindBySearchRequest;
import com.github.ssullivan.db.postgres.PgServiceDao;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool.PoolInitializationException;
import javax.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PsqlClientModule extends DropwizardAwareModule<AppConfig> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PsqlClientModule.class);
  private final DatabaseConfig psqlConfig;

  public PsqlClientModule(final DatabaseConfig psqlConfig) {
    this.psqlConfig = psqlConfig;
  }

  static String generateAuthToken(RdsConfig rdsConfig) {
    final String authToken = generateAuthToken(rdsConfig.getRegion(), rdsConfig.getHost(),
        rdsConfig.getPort(), rdsConfig.getUsername());
    LOGGER.info("Successfully, generated auth token for RDS!");
    return authToken;
  }

  static String generateAuthToken(String region, String hostName, int port, String username) {
    LOGGER.info("Generating token in {} for {}@{}:{}", region, username, hostName, port);

    final RdsIamAuthTokenGenerator generator = RdsIamAuthTokenGenerator.builder()
        .credentials(AWSCredentialsProviderChain.getInstance())
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
    LOGGER.info("Setting up database pool for {}:{}/{}", this.psqlConfig.getHost(),
        this.psqlConfig.getPort(), this.psqlConfig.getDatabaseName());

    final HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");

    hikariConfig.setUsername(psqlConfig.getUsername());
    hikariConfig
        .setPassword(this.psqlConfig.getPassword() == null ? "" : this.psqlConfig.getPassword());

    hikariConfig.addDataSourceProperty("serverName", this.psqlConfig.getHost());
    hikariConfig.addDataSourceProperty("databaseName", this.psqlConfig.getDatabaseName());
    hikariConfig.addDataSourceProperty("portNumber", this.psqlConfig.getPort());

    if (this.psqlConfig.getSsl()) {
      LOGGER.info("Configuring psql client with SSL support");
    } else {
      LOGGER.info("Postgres client is not configured for SSL support");
    }

    hikariConfig.addDataSourceProperty("ssl", this.psqlConfig.getSsl());

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
      hikariConfig
          .setPassword(this.psqlConfig.getPassword() == null ? "" : this.psqlConfig.getPassword());
    } else {
      LOGGER.info("Successfully set password from IAM");
    }

    hikariConfig.setPoolName("api-postgres-pool");
    return hikariConfig;
  }

  @Provides
  @Singleton
  HikariDataSource provideDataSource(final HikariConfig hikariConfig) {
    try {
      LOGGER.info("Creating database pool!");
      return new HikariDataSource(hikariConfig);
    } catch (PoolInitializationException e) {
      LOGGER.error(
          "Failed to connect to database: " + psqlConfig.getHost() + ":" + psqlConfig.getPort()
              + "/" + psqlConfig.getDatabaseName() + "!", e);
      throw e;
    }
  }

  private static class AWSCredentialsProviderChain extends
      com.amazonaws.auth.AWSCredentialsProviderChain {

    private static final AWSCredentialsProviderChain INSTANCE
        = new AWSCredentialsProviderChain();

    AWSCredentialsProviderChain() {
      super(new DefaultAWSCredentialsProviderChain(),
          InstanceProfileCredentialsProvider.getInstance());
    }

    public static AWSCredentialsProviderChain getInstance() {
      return INSTANCE;
    }
  }
}
