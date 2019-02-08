package com.github.ssullivan.tasks;

import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.guice.RedisClientModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import java.io.File;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class LoadCategoriesAndServicesTask extends Command {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoadCategoriesAndServicesTask.class);

  public LoadCategoriesAndServicesTask() {
    super("load-categories-and-services", "Loads categories and schemas into the database");
  }

  @Override
  public void configure(Subparser subparser) {
    subparser.addArgument("-f", "--file")
        .dest("File")
        .required(true)
        .type(File.class)
        .help("Loads category and services from a NDJSON (newline delimited JSON file");

    subparser.addArgument("--host")
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
    final LoadCategoriesAndServicesFunctor loadCategoriesAndServicesFunctor =
        injector.getInstance(LoadCategoriesAndServicesFunctor.class);
    loadCategoriesAndServicesFunctor.loadFile(namespace.get("File"));
  }
}
