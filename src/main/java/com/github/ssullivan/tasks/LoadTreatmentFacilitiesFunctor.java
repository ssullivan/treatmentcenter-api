package com.github.ssullivan.tasks;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.SamshaFacility;
import com.github.ssullivan.utils.ShortUuid;
import com.google.common.collect.Sets;
import io.dropwizard.jackson.Jackson;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import javax.inject.Inject;

public class LoadTreatmentFacilitiesFunctor {
  private IFacilityDao facilityDao;
  private ICategoryCodesDao categoryCodesDao;

  private final ObjectMapper objectMapper = Jackson.newMinimalObjectMapper();

  private final ObjectReader objectReader = objectMapper.readerFor(SamshaFacility.class);

  @Inject
  public LoadTreatmentFacilitiesFunctor(IFacilityDao facilityDao,
      ICategoryCodesDao categoryCodesDao) {
    this.facilityDao = facilityDao;
    this.categoryCodesDao = categoryCodesDao;
  }

  public void run(File file) throws IOException {
    boolean isGzipFile = file.getName().endsWith("gz");
    try (FileInputStream fileInputStream = new FileInputStream(file)) {
      if (isGzipFile) {
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream)) {
          loadStream(gzipInputStream);
        }
      } else {
        loadStream(fileInputStream);
      }
    }
  }

  public void loadStream(final InputStream inputStream) throws IOException {
    processRows(objectReader.readValues(inputStream));
  }

  private void processRows(final MappingIterator<SamshaFacility> iterator) throws IOException {

    while (iterator.hasNextValue()) {
      final SamshaFacility value = iterator.nextValue();
      final Facility facility = new Facility();
      facility.setId(ShortUuid.randomShortUuid());
      facility.setCategoryCodes(value.getCategoryCodes());
      facility.setServiceCodes(value.getServiceCodes());
      facility.setName1(value.getName1());
      facility.setName2(value.getName2());
      facility.setState(value.getState());
      facility.setCity(value.getCity());
      facility.setStreet(value.getStreet1());
      facility.setZip(value.getZip());
      facility.setLocation(value.getLocation());
      facility.setGooglePlaceId(value.getGooglePlaceId());
      facility.setPhoneNumbers(Sets.newHashSet(value.getPhone()));
      facility.setWebsite(value.getWebsite());

      facilityDao.addFacility(facility);
    }
  }
}
