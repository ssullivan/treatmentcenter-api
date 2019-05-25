package com.github.ssullivan;

import com.github.ssullivan.DatabaseConfig;
import com.github.ssullivan.RdsConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;

public final class PostgresTestUtils {

    public static final DatabaseConfig DbConfig = new RdsConfig();
    public static final String BuildEnv = System.getenv("TRAVIS");
    public static final String TestSchema = "testruns";

    static {
        if (BuildEnv != null) {
            DbConfig.setUsername("travis_ci_user");
            DbConfig.setPassword("travis");
            DbConfig.setHost("localhost");
            DbConfig.setDatabaseName("travis_ci_test");
            DbConfig.setPort(Integer.parseInt(System.getenv("PGPORT")));
            DbConfig.setSchema(TestSchema);
        }
        else {
            DbConfig.setUsername("postgres");
            DbConfig.setPassword("");
            DbConfig.setHost("localhost");
            DbConfig.setDatabaseName("app_dev");
            DbConfig.setSchema(TestSchema);
        }
    }

    public static void setupSchema(DSLContext dslContext, final HikariDataSource dataSource) {
        dslContext.createSchema(TestSchema).execute();
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(TestSchema)
                .load();

        flyway.migrate();
    }

    public static void dropSchema(DSLContext dslContext) {
        dslContext.dropSchema(TestSchema).cascade().execute();
    }
}
