package com.github.ssullivan.tasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.guice.RedisClientModule;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.GeoPoint;
import com.github.ssullivan.model.SamshaFacility;
import com.github.ssullivan.model.ServiceCategoryCode;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.cli.Command;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Bootstrap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class LoadTreatmentFacilities extends Command {

  public LoadTreatmentFacilities() {
    super("load-treatment-centers", "Loads treatment center details into the database");
  }

  @Override
  public void configure(Subparser subparser) {
    subparser.addArgument("-f", "--file")
        .dest("File")
        .required(true)
        .type(File.class)
        .help("Loads treatment centers from a NDJSON (newline delimited JSON file");

    subparser.addArgument( "--host")
        .dest("Host")
        .required(false)
        .setDefault("localhost")
        .type(String.class)
        .help("The IP address or hostname of the REDIS server (defaults to localhost)");

    subparser.addArgument("-p", "--port")
        .dest("Port")
        .required(false)
        .setDefault(6379)
        .type(Integer.class)
        .help("The port number of the REDIS server (defaults to 6379)");
  }

  @Override
  public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
    final RedisConfig redisConfig = new RedisConfig();
    redisConfig.setHost(namespace.getString("Host"));
    redisConfig.setPort(namespace.getInt("Port"));

    final Injector injector = Guice.createInjector(new RedisClientModule(redisConfig));
    final IFacilityDao facilityDao = injector.getInstance(IFacilityDao.class);
    final ICategoryCodesDao categoryCodesDao = injector.getInstance(ICategoryCodesDao.class);

    final ObjectMapper objectMapper = Jackson.newMinimalObjectMapper();

    final ObjectReader objectReader = objectMapper.readerFor(SamshaFacility.class);

    final File file = namespace.get("File");
    boolean isGzipFile = file.getName().endsWith("gz");

    try (FileInputStream fileInputStream = new FileInputStream((File) namespace.get("File"))) {
      if (isGzipFile) {
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream)) {
          processRows(facilityDao, categoryCodesDao, objectReader.readValues(gzipInputStream));
        }
      }
      else {
        processRows(facilityDao, categoryCodesDao, objectReader.readValues(fileInputStream));
      }
    }
  }

  private void processRows(final IFacilityDao facilityDao, final ICategoryCodesDao categoryCodesDao,
      final MappingIterator<SamshaFacility> iterator) throws IOException {

    while (iterator.hasNextValue()) {
      final SamshaFacility value = iterator.nextValue();
      final Facility facility = new Facility();
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