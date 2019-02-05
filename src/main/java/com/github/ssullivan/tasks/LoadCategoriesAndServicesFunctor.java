package com.github.ssullivan.tasks;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.db.IServiceCodesDao;
import com.github.ssullivan.model.Category;
import com.github.ssullivan.model.Service;
import com.github.ssullivan.model.ServiceCategoryCode;
import io.dropwizard.jackson.Jackson;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class LoadCategoriesAndServicesFunctor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(LoadCategoriesAndServicesFunctor.class);

  private ICategoryCodesDao categoryCodesDao;
  private IServiceCodesDao serviceCodesDao;
  private final ObjectMapper objectMapper = Jackson.newMinimalObjectMapper();

  private final ObjectReader objectReader = objectMapper.readerFor(ServiceCategoryCode.class);
  private final Map<String, Category> categoryMap = new HashMap<>(128);
  private final Map<String, Set<String>> categoryServiceMap = new HashMap<>(128);

  @Inject
  LoadCategoriesAndServicesFunctor(ICategoryCodesDao categoryCodesDao,
      IServiceCodesDao serviceCodesDao) {
    this.categoryCodesDao = categoryCodesDao;
    this.serviceCodesDao = serviceCodesDao;
  }

  public void loadFile(final File file) throws IOException {
    try (FileInputStream fileInputStream = new FileInputStream(file)) {
      loadStream(fileInputStream);
    }
  }

  public void loadStream(final InputStream inputStream) throws IOException {
    MappingIterator<ServiceCategoryCode> iterator = objectReader.readValues(inputStream);
    while (iterator.hasNextValue()) {
      final ServiceCategoryCode value = iterator.nextValue();

      final Category category = new Category();
      category.setCode(value.getCategoryCode());
      category.setName(value.getCategoryName());
      categoryMap.put(value.getCategoryCode(), category);

      Set<String> services = categoryServiceMap
          .computeIfAbsent(value.getCategoryCode(), k -> new HashSet<>());

      services.add(value.getServiceCode());

      final Service service = new Service();
      service.setCategoryCode(value.getCategoryCode());
      service.setCode(value.getServiceCode());
      service.setName(value.getServiceName());
      service.setDescription(value.getServiceDescription());

      serviceCodesDao.addService(service);
    }

    for (Category category : categoryMap.values()) {
      Set<String> services = categoryServiceMap.get(category.getCode());
      category.setServiceCodes(services);
      categoryCodesDao.addCategory(category);
    }
  }

}
