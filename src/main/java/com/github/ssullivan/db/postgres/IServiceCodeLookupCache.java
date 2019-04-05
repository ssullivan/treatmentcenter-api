package com.github.ssullivan.db.postgres;

import com.google.inject.ImplementedBy;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@ImplementedBy(ServiceCodeLookupCache.class)
public interface IServiceCodeLookupCache {
    Integer lookup(final String serviceCode) throws ExecutionException;

    Set<Integer> lookupSet(final Collection<String> serviceCodes);

    Set<Integer> lookupSet(final String... serviceCodes);
}
