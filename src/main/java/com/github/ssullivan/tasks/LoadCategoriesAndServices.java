package com.github.ssullivan.tasks;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.db.ICategoryCodesDao;
import com.github.ssullivan.db.IServiceCodesDao;
import com.github.ssullivan.guice.RedisClientModule;
import com.github.ssullivan.model.Category;
import com.github.ssullivan.model.Service;
import com.github.ssullivan.model.ServiceCategoryCode;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.cli.Command;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Bootstrap;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadCategoriesAndServices extends Command {
  private static final Logger LOGGER = LoggerFactory.getLogger(LoadCategoriesAndServices.class);

  public LoadCategoriesAndServices() {
    super("load-categories-and-services", "Loads categories and schemas into the database");
  }

  @Override
  public void configure(Subparser subparser) {
    subparser.addArgument("-f", "--file")
        .dest("File")
        .required(true)
        .type(File.class)
        .help("Loads category and services from a NDJSON (newline delimited JSON file");

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
    final ICategoryCodesDao categoryCodesDao = injector.getInstance(ICategoryCodesDao.class);
    final IServiceCodesDao serviceCodesDao = injector.getInstance(IServiceCodesDao.class);
    final ObjectMapper objectMapper = Jackson.newMinimalObjectMapper();

    final ObjectReader objectReader = objectMapper.readerFor(ServiceCategoryCode.class);

    final Map<String, Category> categoryMap = new HashMap<>(128);
    final Map<String, Set<String>> categoryServiceMap = new HashMap<>(128);

    try (FileInputStream fileInputStream = new FileInputStream((File) namespace.get("File"))) {
      MappingIterator<ServiceCategoryCode> iterator = objectReader.readValues(fileInputStream);
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

    LOGGER.debug("Finished");

  }
}
