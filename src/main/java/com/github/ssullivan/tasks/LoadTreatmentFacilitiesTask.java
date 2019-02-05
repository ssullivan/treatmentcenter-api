package com.github.ssullivan.tasks;

import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.db.IFeedDao;
import com.github.ssullivan.guice.RedisClientModule;
import com.github.ssullivan.utils.ShortUuid;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import java.io.File;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class LoadTreatmentFacilitiesTask extends Command {

  public LoadTreatmentFacilitiesTask() {
    super("load-treatment-centers", "Loads treatment center details into the database");
  }

  @Override
  public void configure(Subparser subparser) {
    subparser.addArgument("-f", "--file")
        .dest("File")
        .required(true)
        .type(File.class)
        .help("Loads treatment centers from a NDJSON (newline delimited JSON file");

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

    final File file = namespace.get("File");


    injector.getInstance(LoadTreatmentFacilitiesFunctor.class)
        .run(file);
  }
}
