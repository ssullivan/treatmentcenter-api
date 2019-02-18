package com.github.ssullivan.tasks.feeds;

import com.amazonaws.services.s3.AmazonS3;
import com.github.ssullivan.AppConfig;
import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.guice.AwsS3ClientModule;
import com.github.ssullivan.guice.CrawlDelay;
import com.github.ssullivan.guice.RedisClientModule;
import com.github.ssullivan.model.aws.AwsS3Settings;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import java.io.File;
import java.io.IOException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadSamshaCommand extends ConfiguredCommand<AppConfig> {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoadSamshaCommand.class);
  private Injector injector;

  public LoadSamshaCommand() {
    super("load-samsha", "Loads treatment center details into the database");
  }

  @Override
  public void configure(Subparser subparser) {
    super.configure(subparser);

    subparser.addArgument("-f", "--file")
        .dest("File")
        .required(false)
        .type(File.class)
        .help("Path to the locator spreadsheet on disk");

    subparser.addArgument("-u", "--url")
        .dest("Url")
        .required(false)
        .type(String.class)
        .setDefault("https://findtreatment.samhsa.gov")
        .help("URL to the SAMSHA locator spreasheet");

    subparser.addArgument("-a", "--accesskey")
        .dest("AccessKey")
        .required(false)
        .type(String.class)
        .setDefault("")
        .help("The AWS access key to use.");

    subparser.addArgument("-s", "--secretkey")
        .dest("SecretKey")
        .required(false)
        .type(String.class)
        .setDefault("")
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
        // for dev set to http://localhost:9000
        .setDefault("")
        .type(String.class)
        .help("The AWS API endpoint");

    subparser.addArgument("--host")
        .dest("Host")
        .required(false)
        .setDefault(System.getenv("REDIS_HOST"))
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
  protected void run(Bootstrap<AppConfig> bootstrap, Namespace namespace, AppConfig configuration)
      throws Exception {
    try {
      LOGGER.info("Started");

      final Runtime runtime = Runtime.getRuntime();
      LOGGER
          .info("Configured to run with total memory {} / free memory {} / max memory {} / cpu {}",
              runtime.totalMemory(),
              runtime.freeMemory(),
              runtime.maxMemory(),
              runtime.availableProcessors());

      RedisConfig redisConfig = new RedisConfig();

      redisConfig.setHost(namespace.getString("Host"));
      redisConfig.setPort(namespace.getInt("Port"));
      redisConfig.setDb(namespace.getInt("Database"));

      LOGGER.info("[redis] Host is {}", redisConfig.getHost());
      LOGGER.info("[redis] Port is {}", redisConfig.getPort());

      final String awsAccessKey = namespace.getString("AccessKey");
      final String awsSecretKey = namespace.getString("SecretKey");
      final String awsBucket = namespace.getString("Bucket");
      final String awsRegion = namespace.getString("Region");

      final AwsS3Settings settings = new AwsS3Settings(awsSecretKey, awsAccessKey,
          namespace.getString("Endpoint"), awsRegion,
          awsBucket);

      final File spreadsheetFile = namespace.get("File");
      final String spreadheetUrl = namespace.get("Url");
      String locatorUrl = "";

      if (spreadsheetFile != null) {
        locatorUrl = spreadsheetFile.toURI().toURL().toString();
      } else {
        locatorUrl = spreadheetUrl;
      }

      LOGGER.info("[samsha] Downloading locator.xlsx from {}", locatorUrl);

      this.injector = Guice.createInjector(new RedisClientModule(redisConfig),
          new AbstractModule() {
            @Override
            protected void configure() {
              bindConstant().annotatedWith(CrawlDelay.class).to(4096L);
            }
          },
          new AwsS3ClientModule(settings, locatorUrl));

      // Verify that things are working

      // (1) Create an AmazonS3 client
      final AmazonS3 amazonS3 = this.injector.getInstance(AmazonS3.class);

      // (2) Check the redis db
      final RedisClient client = this.injector.getInstance(RedisClient.class);

      try (StatefulRedisConnection<String, String> conn = client.connect()) {
        final String pingResponse = conn.sync().ping();
        LOGGER.info("[redis] Ping response was {}", pingResponse);
      }

      LOGGER.info("Successfully, connected to Elasticache/Redis {}", redisConfig.getHost());
      final ISamshaEtlJob samshaEtlJob = injector.getInstance(ISamshaEtlJob.class);
      samshaEtlJob.extract();
      samshaEtlJob.transform();
      samshaEtlJob.load();

    } catch (IOException e) {
      LOGGER.error("Failed to fetch / transform / load SAMSHSA data", e);
    } finally {
      if (this.injector != null) {
        try {
          this.injector.getInstance(RedisClient.class).shutdown();
          LOGGER.info("Shutdown redis client");
        } finally {
          this.injector.getInstance(AmazonS3.class).shutdown();
          LOGGER.info("Shutdown AmazonS3 client");
        }
      }
      LOGGER.info("Done");
    }
  }

}
