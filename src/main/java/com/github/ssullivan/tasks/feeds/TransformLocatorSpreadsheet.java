package com.github.ssullivan.tasks.feeds;

import com.github.ssullivan.model.Category;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.GeoPoint;
import com.github.ssullivan.model.Service;
import com.github.ssullivan.model.collections.Tuple2;
import com.github.ssullivan.model.datafeeds.SamshaLocatorData;
import com.github.ssullivan.utils.ShortUuid;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class TransformLocatorSpreadsheet implements
    BiFunction<String, InputStream, Optional<SamshaLocatorData>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransformLocatorSpreadsheet.class);


  private static final int FACILITIES_WITH_SERVICE_CODE_DETAIL = 0;
  private static final int SERVICE_CODER_REFERENCE = 1;

  private static List<Facility> transformToFacilities(final String feedId,
      final ImmutableList<ImmutableMap<String, String>> locations,
      final ImmutableList<ImmutableMap<String, String>> serviceCodes) {
    return locations.stream()
        .map(it -> asFacility(it, serviceCodes))
        .peek(it -> it.setFeedId(feedId))
        .collect(Collectors.toList());
  }

  private static Tuple2<Collection<Category>, Collection<Service>> transformToCategoriesAndServices(
      final ImmutableList<ImmutableMap<String, String>> catsAndServices) {
    final Map<String, Category> cats = new HashMap<>(catsAndServices.size());
    final List<Service> services = new ArrayList<Service>();

    catsAndServices
        .forEach(row -> {
          final Category category = cats
              .computeIfAbsent(row.get("category_code"), (k) -> asCategory(row));
          final Service service = asService(row);

          category.addServiceCode(service);
          services.add(service);
        });

    return new Tuple2<>(cats.values(), services);
  }

  private static Service asService(final ImmutableMap<String, String> categoryAndService) {
    final Service service = new Service();
    service.setCategoryCode(categoryAndService.getOrDefault("category_code", ""));
    service.setCode(categoryAndService.getOrDefault("service_code", "").intern());
    service.setName(categoryAndService.getOrDefault("service_name", ""));
    service.setDescription(categoryAndService.getOrDefault("service_description", ""));
    return service;
  }

  private static Category asCategory(final ImmutableMap<String, String> categoryAndService) {
    final Category category = new Category();
    category.setCode(categoryAndService.getOrDefault("category_code", "").intern());
    category.setName(categoryAndService.getOrDefault("category_name", ""));
    return category;
  }

  private static Facility asFacility(final ImmutableMap<String, String> location,
      final ImmutableList<ImmutableMap<String, String>> serviceCodes) {
    final Facility facility = new Facility();
    facility.setId(ShortUuid.randomShortUuid());
    facility.setName1(location.get("name1"));
    facility.setName2(location.get("name2"));
    facility.setStreet(getStreet(location));
    facility.setCity(location.get("city").intern());
    facility.setState(location.get("state").intern());
    facility.setZip(getZip(location));
    facility.setCounty(location.get("county"));
    facility.setPhoneNumbers(getPhoneNumbers(location));
    facility.setWebsite(location.get("website"));
    facility.setLocation(geoPoint(location));

    final Set<String> services = serviceCodes
        .stream()
        .map(it -> it.getOrDefault("service_code", ""))
        .filter(it -> !it.isEmpty())
        .map(String::toLowerCase)
        .filter(it -> hasService(location, it))
        .map(String::toUpperCase)
        .collect(Collectors.toSet());

    final Set<String> cats = serviceCodes
        .stream()
        .filter(it -> {
          final String serviceCode = it.getOrDefault("service_code", "").toUpperCase();
          return services.contains(serviceCode);
        })
        .map(it -> it.getOrDefault("category_code", ""))
        .filter(it -> !it.isEmpty())
        .collect(Collectors.toSet());

    facility.setServiceCodes(services);
    facility.setCategoryCodes(cats);

    return facility;
  }

  private static boolean hasService(final ImmutableMap<String, String> location,
      final String serviceCode) {
    final String cellValue = location.getOrDefault(serviceCode.toLowerCase(), "");
    if (cellValue.isEmpty()) {
      return false;
    } else if ("1".equalsIgnoreCase(cellValue)) {
      return true;
    }

    LOGGER.warn("Found unknown value for service '{}' of '{}'", serviceCode, cellValue);
    return false;
  }

  private static GeoPoint geoPoint(final ImmutableMap<String, String> location) {
    final String latOrig = location.getOrDefault("latitude", "");
    final String lonOrig = location.getOrDefault("longitude", "");

    if (latOrig.isEmpty() || lonOrig.isEmpty()) {
      return null;
    }

    final double lat = Double.parseDouble(latOrig);
    final double lon = Double.parseDouble(lonOrig);
    return GeoPoint.geoPoint(lat, lon);
  }

  private static Set<String> getPhoneNumbers(final ImmutableMap<String, String> location) {
    final Set<String> phoneNumbers = new HashSet<>();
    final String phone = location.getOrDefault("phone", "");
    if (!phone.isEmpty()) {
      phoneNumbers.add(phone);
    }

    final String intake1 = location.getOrDefault("intake1", "");
    if (!intake1.isEmpty()) {
      phoneNumbers.add(intake1);
    }

    final String intake2 = location.getOrDefault("intake2", "");
    if (!intake1.isEmpty()) {
      phoneNumbers.add(intake2);
    }

    return phoneNumbers;
  }

  private static String getZip(final ImmutableMap<String, String> zipCode) {
    final String zip = zipCode.getOrDefault("zip", "");
    final String zip4 = zipCode.getOrDefault("zip4", "");
    if (zip.isEmpty()) {
      return "";
    }

    if (zip4.isEmpty()) {
      return zip;
    }
    return zip + "-" + zip4;
  }

  private static String getStreet(final ImmutableMap<String, String> location) {
    final String street1 = location.getOrDefault("street1", "");
    final String street2 = location.getOrDefault("street2", "");

    if (street2.isEmpty()) {
      return street1;
    }
    return street1 + ", " + street2;
  }

  private static ImmutableList<ImmutableMap<String, String>> collectMaps(final Sheet sheet) {
    final ImmutableList.Builder<ImmutableMap<String, String>> builder = new ImmutableList.Builder<>();

    final Row headerRow = sheet.getRow(0);
    final ImmutableMap<Integer, String> indexToHeader = indexToHeaderMap(headerRow);

    int i = 0;
    final Iterator<Row> itty = sheet.rowIterator();
    while (itty.hasNext()) {
      final Row row = itty.next();
      if (i > 0) {
        builder.add(toMap(indexToHeader, row));
      }
      i++;
    }

    return builder.build();
  }

  /**
   * Maps the row into a dictionary of field name => field value
   *
   * @param headers the headers so we can associate values to the correct field
   * @param row the current row containing a facility
   * @return a non-null list of immutable maps
   */
  private static ImmutableMap<String, String> toMap(final ImmutableMap<Integer, String> headers,
      final Row row) {
    final ImmutableMap.Builder<String, String> builder = new Builder<>();
    int i = 0;
    final Iterator<Cell> itty = row.cellIterator();
    while (itty.hasNext()) {
      final Cell cell = itty.next();
      builder.put(headers.getOrDefault(i, "UNKNOWN_" + i), cell.getStringCellValue().trim());
      i++;
    }

    return builder.build();
  }

  /**
   * Creates a dictionary of row index to header field name
   *
   * @param row the header row
   * @return a non-null map of the headers
   */
  private static ImmutableMap<Integer, String> indexToHeaderMap(final Row row) {
    final ImmutableMap.Builder<Integer, String> builder = new Builder<>();
    int i = 0;
    final Iterator<Cell> itty = row.cellIterator();
    while (itty.hasNext()) {
      final Cell cell = itty.next();
      builder.put(i, cell.getStringCellValue().trim());
      i++;
    }

    return builder.build();
  }

  @Override
  public Optional<SamshaLocatorData> apply(final String feedId, final InputStream inputStream) {
    Objects.requireNonNull(feedId, "The feedId must not be empty");
    Objects.requireNonNull(inputStream, "The spreadsheet stream must not be empty");

    LOGGER.info("Parsing spreadsheet for feed {}", feedId);
    try (final Workbook workbook = WorkbookFactory.create(inputStream)) {

      final Sheet facilitySheet = workbook.getSheetAt(FACILITIES_WITH_SERVICE_CODE_DETAIL);
      final Sheet serviceCodeSheet = workbook.getSheetAt(SERVICE_CODER_REFERENCE);

      final ImmutableList<ImmutableMap<String, String>> locations = collectMaps(facilitySheet);
      final ImmutableList<ImmutableMap<String, String>> serviceCodes = collectMaps(
          serviceCodeSheet);

      final Tuple2<Collection<Category>, Collection<Service>> services = transformToCategoriesAndServices(
          serviceCodes);
      final List<Facility> facilities = transformToFacilities(feedId, locations,
          serviceCodes);

      return Optional
          .of(new SamshaLocatorData(feedId, services.get_1(), services.get_2(), facilities));
    } catch (IOException e) {
      LOGGER.error("Failed to process Workbook", e);
    } finally {
      LOGGER.info("Finished parsing data for feed {}", feedId);
    }
    return Optional.empty();
  }

}
