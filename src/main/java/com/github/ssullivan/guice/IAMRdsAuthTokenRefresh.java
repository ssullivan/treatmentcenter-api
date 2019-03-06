package com.github.ssullivan.guice;

import com.amazonaws.SdkClientException;
import com.github.ssullivan.RdsConfig;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zaxxer.hikari.HikariDataSource;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IAMRdsAuthTokenRefresh extends AbstractScheduledService implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(IAMRdsAuthTokenRefresh.class);

    private HikariDataSource hikariDataSource;
    private RdsConfig rdsConfig;

    @Inject
    public IAMRdsAuthTokenRefresh(final HikariDataSource hikariDataSource, final RdsConfig rdsConfig) {
        this.hikariDataSource = hikariDataSource;
        this.rdsConfig = rdsConfig;

    }

    @Override
    protected void runOneIteration() throws Exception {
        if (this.rdsConfig.isIamAuth()) {
            try {
                this.hikariDataSource.getHikariConfigMXBean().setPassword(
                        PsqlClientModule.generateAuthToken(rdsConfig));
            }
            catch (SdkClientException e) {
                LOGGER.error("!!! Failed to generate new RDS auth token !!!", e);
            }
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedDelaySchedule(10, 10, TimeUnit.MINUTES);
    }

    @Override
    public void start() throws Exception {
        this.startAsync();
    }

    @Override
    public void stop() throws Exception {
        this.stopAsync();
    }
}
