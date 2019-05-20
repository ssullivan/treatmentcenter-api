package com.github.ssullivan;

import com.google.common.collect.Sets;
import org.apache.commons.net.util.SubnetUtils;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class GSuiteConstants {
    private static final Set<SubnetUtils> GOOGLE_IP_CIDRS = Collections.unmodifiableSet(Sets.newHashSet("64.18.0.0/20",
            "64.233.160.0/19",
            "66.102.0.0/20",
            "66.249.80.0/20",
            "72.14.192.0/18",
            "74.125.0.0/16",
            "173.194.0.0/16",
            "207.126.144.0/20",
            "209.85.128.0/17",
            "216.239.32.0/19").stream().map(SubnetUtils::new)
            .collect(Collectors.toSet()));

    public boolean isGSuiteIpAddress(final String ipAddress) {
        if (null == ipAddress || ipAddress.isEmpty()) return false;
        return GOOGLE_IP_CIDRS.stream().anyMatch(it -> it.getInfo().isInRange(ipAddress));
    }
}
