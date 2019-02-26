package com.github.ssullivan.guice;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.rds.auth.GetIamAuthTokenRequest;
import com.amazonaws.services.rds.auth.RdsIamAuthTokenGenerator;
import com.github.ssullivan.RdsConfig;
import com.google.inject.Provides;
import com.zaxxer.hikari.HikariConfig;

public class RdsClientModule extends PsqlClientModule {
    private final RdsConfig rdsConfig;

    public RdsClientModule(final RdsConfig rdsConfig) {
        super(rdsConfig);
        this.rdsConfig = rdsConfig;
    }

    @Override
    protected void configure() {

    }

    @Provides
    HikariConfig providesHikariConfig() {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        hikariConfig.setUsername(rdsConfig.getUsername());
        hikariConfig.setPassword(generateAuthToken(rdsConfig));

        hikariConfig.addDataSourceProperty("dataSource.user", this.rdsConfig.getUsername());
        hikariConfig.addDataSourceProperty("dataSource.databaseName", this.rdsConfig.getDatabaseName());
        hikariConfig.setPoolName("api-postgres-pool");
        return hikariConfig;
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
