package com.github.ssullivan.tasks.feeds;

import com.amazonaws.services.s3.AmazonS3;
import com.github.ssullivan.AppConfig;
import com.github.ssullivan.RdsConfig;
import com.github.ssullivan.RedisConfig;
import com.github.ssullivan.guice.AwsS3ClientModule;
import com.github.ssullivan.guice.CrawlDelay;
import com.github.ssullivan.guice.PsqlClientModule;
import com.github.ssullivan.guice.RdsClientModule;
import com.github.ssullivan.guice.RedisClientModule;
import com.github.ssullivan.model.aws.AwsS3Settings;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.zaxxer.hikari.HikariDataSource;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadSamshaCommandPostgres extends ConfiguredCommand<AppConfig> {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoadSamshaCommandRedis.class);
  private Injector injector;

  public LoadSamshaCommandPostgres() {
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
        .setDefault(System.getenv("PG_HOST"))
        .type(String.class)
        .help("The IP address or hostname of the POSTGRES server (defaults to localhost)");

    subparser.addArgument("-p", "--port")
        .dest("Port")
        .required(false)
        .setDefault(5432)
        .type(Integer.class)
        .help("The port number of the POSTGRES server (defaults to 5432)");

    subparser.addArgument("-d", "--database")
        .dest("Database")
        .required(false)
        .setDefault(0)
        .type(Integer.class)
        .help("The postgres database to store the data into (default 0)");

    subparser.addArgument("-u", "--username")
        .dest("Username")
        .required(false)
        .setDefault("postgres")
        .type(String.class);

    subparser.addArgument("-p", "--password")
        .dest("Password")
        .required(false)
        .setDefault(Optional.ofNullable(System.getenv("PG_PASSWORD")).orElse(""))
        .type(String.class);

    subparser.addArgument("-useIAM")
        .dest("UseIAM")
        .help("Set this flag to control what to use for authenticating to the database backend.")
        .required(false)
        .setDefault(false)
        .type(Boolean.class);

    subparser.addArgument("-skipS3")
        .dest("SkipS3")
        .required(false)
        .setDefault(false)
        .type(Boolean.class);
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

      final RdsConfig rdsConfig = new RdsConfig();
      rdsConfig.setDatabaseName(namespace.getString("Database"));
      rdsConfig.setRegion(namespace.getString("Region"));
      rdsConfig.setPassword(namespace.getString("Password"));
      rdsConfig.setUsername(namespace.getString("Username"));
      rdsConfig.setHost(namespace.getString("Host"));
      rdsConfig.setPort(namespace.getInt("Port"));


      LOGGER.info("[redis] Host is {}", rdsConfig.getHost());
      LOGGER.info("[redis] Port is {}", rdsConfig.getPort());

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



      this.injector = Guice.createInjector(new AbstractModule() {
        @Override
        protected void configure() {
          // If we selected IAM we are running in AWS
          if (namespace.get("UseIAM")) {
            LOGGER.info("Configuring application for RDS");
            install(new RdsClientModule(rdsConfig));
          }
          else {
            LOGGER.info("Configuring application for POSTGRES");
            install(new PsqlClientModule(rdsConfig));
          }
        }
      },
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
      LOGGER.info("Region is {}", amazonS3.getRegion());

      // (2) Check the postgres db
      final HikariDataSource client = this.injector.getInstance(HikariDataSource.class);
      if (client.isRunning()) {
        LOGGER.info("Connected to postgres/rds backend!");
      }

      LOGGER.info("Successfully, connected to Postgres/RDS {}", rdsConfig.getHost());
      if (namespace.get("SkipS3")) {
        final ISamshaEtlJob samshaEtlJob = injector.getInstance(ISamshaEtlJob.class);

        samshaEtlJob.extract();
        samshaEtlJob.transform();
        samshaEtlJob.load();
      }
      else {
        final ISamshaEtlJob samshaEtlJob = injector.getInstance(InMemorySamshaLocalEtl.class);

        samshaEtlJob.extract();
        samshaEtlJob.transform();
        samshaEtlJob.load();
      }

    } catch (IOException e) {
      LOGGER.error("Failed to fetch / transform / load SAMSHSA data", e);
    } finally {
      if (this.injector != null) {
        try {
          this.injector.getInstance(HikariDataSource.class).close();
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
