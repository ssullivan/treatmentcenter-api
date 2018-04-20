package com.github.ssullivan.core;

import com.github.ssullivan.api.IServiceCodesService;
import com.github.ssullivan.db.IServiceCodesDao;
import com.github.ssullivan.model.SearchResults;
import java.io.IOException;
import javax.inject.Inject;

public class ServiceCodesService implements IServiceCodesService {

  private final IServiceCodesDao serviceCodesDao;

  @Inject
  public ServiceCodesService(final IServiceCodesDao serviceCodesDao) {
    this.serviceCodesDao = serviceCodesDao;
  }

  @Override
  public SearchResults listServiceCodes() throws IOException {
    return this.serviceCodesDao.listServiceCodes();
  }

  @Override
  public SearchResults listServiceCodesInCategory(String categoryCode) throws IOException {
    return this.serviceCodesDao.listServiceCodesInCategory(categoryCode);
  }
}
