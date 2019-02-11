package com.github.ssullivan.tasks.feeds;

import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.db.IServiceCodesDao;
import com.github.ssullivan.model.Category;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Service;
import com.github.ssullivan.model.datafeeds.SamshaLocatorData;
import java.io.IOException;
import java.util.function.Function;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoreSamshaLocatorData implements Function<SamshaLocatorData, Boolean> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoreSamshaLocatorData.class);

  private final IFeedDao feedDao;
  private final ICategoryCodesDao categoryCodesDao;
  private final IServiceCodesDao serviceCodesDao;
  private final IFacilityDao facilityDao;

  @Inject
  public StoreSamshaLocatorData(final IFeedDao feedDao,
      final ICategoryCodesDao categoryCodesDao,
      final IServiceCodesDao serviceCodesDao,
      final IFacilityDao facilityDao) {

    this.feedDao = feedDao;
    this.facilityDao = facilityDao;
    this.categoryCodesDao = categoryCodesDao;
    this.serviceCodesDao = serviceCodesDao;
  }

  @Override
  public Boolean apply(final SamshaLocatorData samshaLocatorData) {

    int totalCats = 0;
    for (final Category category : samshaLocatorData.getCategories()) {
      try {
        this.categoryCodesDao.addCategory(samshaLocatorData.getFeedId(), category);
        totalCats++;
      } catch (IOException e) {
        LOGGER.error("Failed to add category: {}", category);
      }
    }
    LOGGER.info("Loaded {} of {} categories", totalCats, samshaLocatorData.getCategories().size());

    int totalServices = 0;
    for (final Service service : samshaLocatorData.getServices()) {
      try {
        this.serviceCodesDao.addService(samshaLocatorData.getFeedId(), service);
        totalServices++;
      } catch (IOException e) {
        LOGGER.error("Failed to add service: {}", service);
      }
    }
    LOGGER.info("Loaded {} of {} services", totalServices, samshaLocatorData.getServices().size());

    int totalLocations = 0;
    for (final Facility facility : samshaLocatorData.getFacilities()) {
      try {
        facilityDao.addFacility(samshaLocatorData.getFeedId(), facility);
        totalLocations++;
      } catch (IOException e) {
        LOGGER.error("Failed to add facility: {}", facility);
      }
    }
    LOGGER.info("Loaded {} of {} locations", totalLocations,
        samshaLocatorData.getFacilities().size());

    return true;
  }
}
