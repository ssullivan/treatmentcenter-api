package com.github.ssullivan.guice;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.zaxxer.hikari.HikariDataSource;
import io.dropwizard.lifecycle.Managed;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IAMRdsAuthTokenRefresh extends AbstractScheduledService implements Managed {
    private HikariDataSource hikariDataSource;

    public IAMRdsAuthTokenRefresh(final HikariDataSource hikariDataSource) {
        this.hikariDataSource = hikariDataSource;
    }

    @Override
    protected void runOneIteration() throws Exception {

    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedDelaySchedule(10, 10, TimeUnit.MINUTES);
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }
}
