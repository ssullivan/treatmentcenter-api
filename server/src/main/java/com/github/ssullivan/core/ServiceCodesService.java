package com.github.ssullivan.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.ssullivan.api.ICategoryAndServiceService;
import com.github.ssullivan.db.IServiceCodesDao;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.model.ServiceCategoryCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.inject.Inject;

public class ServiceCodesService implements ICategoryAndServiceService {
  private final IServiceCodesDao serviceCodesDao;
  private final ObjectReader objectReader;

  @Inject
  public ServiceCodesService(final IServiceCodesDao serviceCodesDao, final ObjectMapper objectMapper) {
    this.serviceCodesDao = serviceCodesDao;
    this.objectReader = objectMapper.readerFor(ServiceCategoryCode.class);
  }

  @Override
  public SearchResults listServiceCodes() throws IOException {
    return this.serviceCodesDao.listServiceCodes();
  }

  @Override
  public SearchResults listServiceCodesInCategory(String categoryCode) throws IOException {
    return this.serviceCodesDao.listServiceCodesInCategory(categoryCode);
  }

  /**
   * I
   * @param inputStream
   */
  @Override
  public void loadServiceCodeAndCategories(InputStream inputStream) throws IOException {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
        final String jsonLine = reader.readLine();
        final ServiceCategoryCode serviceCategoryCode = objectReader.readValue(jsonLine);


    }
  }
}
