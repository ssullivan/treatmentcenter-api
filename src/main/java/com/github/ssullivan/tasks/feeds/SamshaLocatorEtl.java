package com.github.ssullivan.tasks.feeds;

import java.io.IOException;
import javax.inject.Inject;

public class SamshaLocatorEtl implements IEtlJob {
  private FetchSamshaDataFeed fetchSamshaDataFeed;
  private TransformLocatorSpreadsheet transformLocatorSpreadsheet;

  @Inject
  public SamshaLocatorEtl(FetchSamshaDataFeed fetchSamshaDataFeed,
      TransformLocatorSpreadsheet transformLocatorSpreadsheet) {
    this.fetchSamshaDataFeed = fetchSamshaDataFeed;
    this.transformLocatorSpreadsheet = transformLocatorSpreadsheet;
  }

  @Override
  public void extract() throws IOException {
    fetchSamshaDataFeed.run();
  }

  @Override
  public void transform() throws IOException {

  }

  @Override
  public void load() throws IOException {

  }
}
