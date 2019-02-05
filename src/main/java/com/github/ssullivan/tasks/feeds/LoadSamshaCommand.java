package com.github.ssullivan.tasks.feeds;

import com.amazonaws.services.s3.AmazonS3;
import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.guice.AwsS3ClientModule;
import com.github.ssullivan.guice.RedisClientModule;
import com.github.ssullivan.model.aws.AwsS3Settings;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import io.lettuce.core.RedisClient;
import java.io.File;
import java.net.URL;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadSamshaCommand extends Command {
  private static final Logger LOGGER = LoggerFactory.getLogger(LoadSamshaCommand.class);
  private Injector injector;

  public LoadSamshaCommand() {
    super("load-samsha", "Loads treatment center details into the database");
  }

  @Override
  public void configure(Subparser subparser) {
    subparser.addArgument("-f", "--file")
        .dest("File")
        .required(false)
        .type(File.class)
        .help("Path to the locator spreadsheet on disk");

    subparser.addArgument("-u", "--url")
        .dest("Url")
        .required(false)
        .type(URL.class)
        .help("URL to the SAMSHA locator spreasheet");

    subparser.addArgument("-a", "--accesskey")
        .dest("AccessKey")
        .required(false)
        .type(String.class)
        .help("The AWS access key to use.");

    subparser.addArgument("-s", "--secretkey")
        .dest("SecretKey")
        .required(false)
        .type(String.class)
        .help("The AWS secret key to use.");

    subparser.addArgument("-b", "--bucket")
        .dest("Bucket")
        .required(true)
        .type(String.class)
        .help("The AWS bucket to store data into");

    subparser.addArgument("-r", "--region")
        .dest("Region")
        .required(true)
        .setDefault("us-east-1")
        .type(String.class)
        .help("The AWS region that the bucket lives");

    subparser.addArgument("-e", "--endpoint")
        .dest("Endpoint")
        .required(false)
        .setDefault("http://localhost:9000")
        .type(String.class)
        .help("The AWS API endpoint");

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

    subparser.addArgument("-d", "--database")
        .dest("Database")
        .required(false)
        .setDefault(0)
        .type(Integer.class)
        .help("The redis database to store the data into (default 0)");
  }

  @Override
  public void run(Bootstrap<?> bootstrap, Namespace namespace) throws Exception {
    try {
      LOGGER.info("Started");

      final RedisConfig redisConfig = new RedisConfig();
      redisConfig.setHost(namespace.getString("Host"));
      redisConfig.setPort(namespace.getInt("Port"));
      redisConfig.setDb(namespace.getInt("Database"));

      final String awsAccessKey = namespace.getString("AccessKey");
      final String awsSecretKey = namespace.getString("SecretKey");
      final String awsBucket = namespace.getString("Bucket");
      final String awsRegion = namespace.getString("Region");

      final AwsS3Settings settings = new AwsS3Settings(awsSecretKey, awsAccessKey, namespace.getString("Endpoint"), awsRegion,
          awsBucket);

      final File spreadsheetFile = namespace.get("File");
      final URL spreadheetUrl = namespace.get("Url");
      String locatorUrl = "";

      if (spreadsheetFile != null) {
        locatorUrl = spreadsheetFile.toURI().toURL().toString();
      } else {
        locatorUrl = spreadheetUrl.toString();
      }

      this.injector = Guice.createInjector(new RedisClientModule(redisConfig),
          new AwsS3ClientModule(settings, locatorUrl));

      final ISamshaEtlJob samshaEtlJob = injector.getInstance(ISamshaEtlJob.class);
      samshaEtlJob.extract();
      samshaEtlJob.transform();
      samshaEtlJob.load();
    }
    finally {
      try {
        this.injector.getInstance(RedisClient.class).shutdown();
        LOGGER.info("Shutdown redis client");
      }
      finally {
        this.injector.getInstance(AmazonS3.class).shutdown();
        LOGGER.info("Shutdown AmazonS3 client");
      }
      LOGGER.info("Done");
    }
  }

}
