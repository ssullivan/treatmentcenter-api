package com.github.ssullivan.guice;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.github.ssullivan.model.aws.AwsS3Settings;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.security.acl.LastOwnerException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Guice module for providing an AmazonS3 client. Supports using a client with accesskey + secretkey
 * (primarilyu used for local dev with minio) + ec2 instance profiles
 */
public class AwsS3ClientModule extends AbstractModule {
  private static final Logger LOGGER = LoggerFactory.getLogger(AwsS3ClientModule.class);

  private final AwsS3Settings awsS3Settings;
  private final String locatorUrl;

  public AwsS3ClientModule(final AwsS3Settings awsS3Settings, final String url) {
    Objects.requireNonNull(awsS3Settings, "AWS settings must not be empty/null");
    Objects.requireNonNull(url, "SAMSHA locator URL must not be empty/null");
    this.awsS3Settings = awsS3Settings;
    this.locatorUrl = url;
  }

  @Override
  protected void configure() {
    bindConstant().annotatedWith(BucketName.class).to(this.awsS3Settings.getBucket());
    bindConstant().annotatedWith(SamshaUrl.class).to(this.locatorUrl);

    LOGGER.info("[aws] Default bucket is {}", this.awsS3Settings.getBucket());
    LOGGER.info("[aws] Default region is {}", this.awsS3Settings.getRegion());
  }

  @Provides
  @Singleton
  AmazonS3 amazonS3() {
    final AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();

    builder.setCredentials(awsS3Settings.getAWSCredentialsProvider());
    builder.setPathStyleAccessEnabled(true);

    if (this.awsS3Settings.getEndpoint() != null && !this.awsS3Settings.getEndpoint().isEmpty()) {
      ClientConfiguration clientConfiguration = new ClientConfiguration();
      clientConfiguration.setSignerOverride("AWSS3V4SignerType");

      builder.setClientConfiguration(clientConfiguration);

      builder.setEndpointConfiguration(
          new EndpointConfiguration(this.awsS3Settings.getEndpoint(),
              this.awsS3Settings.getRegion())
      );
    } else {
      builder.setRegion(this.awsS3Settings.getRegion());
    }

    return builder.build();
  }
}