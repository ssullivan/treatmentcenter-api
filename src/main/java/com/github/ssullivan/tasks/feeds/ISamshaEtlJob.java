package com.github.ssullivan.tasks.feeds;

import com.google.inject.ImplementedBy;

@ImplementedBy(SamshaLocatorEtl.class)
public interface ISamshaEtlJob extends IEtlJob {

}
