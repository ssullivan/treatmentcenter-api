package com.github.ssullivan.guice;

import com.google.common.collect.ImmutableList;
import io.dropwizard.lifecycle.Managed;

import javax.inject.Inject;
import java.util.List;

public class RdsDropwizardManagedProvider implements IManagedProvider {
    private IAMRdsAuthTokenRefresh iamRdsAuthTokenRefresh;

    @Inject
    public RdsDropwizardManagedProvider(IAMRdsAuthTokenRefresh iamRdsAuthTokenRefresh) {
        this.iamRdsAuthTokenRefresh = iamRdsAuthTokenRefresh;
    }

    @Override
    public List<Managed> get() {
        return ImmutableList.of(this.iamRdsAuthTokenRefresh);
    }
}
