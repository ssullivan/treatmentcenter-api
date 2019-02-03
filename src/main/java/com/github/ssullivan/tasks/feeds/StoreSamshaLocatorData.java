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

    final String feedId;
    try {
      feedId = feedDao.nextFeedId().orElseThrow(() -> new IOException("Failed to generate feed id"));
    } catch (IOException e) {
      LOGGER.error("Failed to generate next feed id for this dataset!", e);
      return false;
    }


    for (final Category category : samshaLocatorData.getCategories()) {
      try {
        this.categoryCodesDao.addCategory(feedId, category);
      }
      catch (IOException e) {
        LOGGER.error("Failed to add category: {}", category);
      }
    }

    for (final Service service : samshaLocatorData.getServices()) {
      try {
        this.serviceCodesDao.addService(feedId, service);
      }
      catch (IOException e) {
        LOGGER.error("Failed to add service: {}", service);
      }
    }


    for (final Facility facility : samshaLocatorData.getFacilities()) {
      try {
        facilityDao.addFacility(facility);
      }
      catch (IOException e) {
        LOGGER.error("Failed to add facility: {}", facility);
      }
    }

    return true;
  }
}
