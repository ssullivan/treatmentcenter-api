package com.github.ssullivan.model.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import java.util.Objects;
import java.util.Optional;

public class AwsS3Settings {

  private final Optional<String> awsSecretKey;
  private final Optional<String> awsAccessKey;
  private final String endpoint;
  private final String region;
  private final String bucket;


  public AwsS3Settings(final String awsSecretKey, final String awsAccessKey, final String endpoint,
      final String region, final String bucket) {
    Objects.requireNonNull(region, "The region must be specified!");
    Objects.requireNonNull(bucket, "The bucket must be specified!");
    this.endpoint = endpoint;
    this.awsSecretKey = Optional.ofNullable(awsSecretKey);
    this.awsAccessKey = Optional.ofNullable(awsAccessKey);
    this.region = region;
    this.bucket = bucket;
  }

  public AwsS3Settings(String region, String bucket) {
    this(null, null, null, region, bucket);
  }

  public AWSCredentialsProvider getAWSCredentialsProvider() {
    if (hasAwsCredentials()) {
      return new AWSStaticCredentialsProvider(new AWSCredentials() {
        @Override
        public String getAWSAccessKeyId() {
          return awsAccessKey.orElse("");
        }

        @Override
        public String getAWSSecretKey() {
          return awsSecretKey.orElse("");
        }
      });
    } else {
      return new InstanceProfileCredentialsProvider(false);
    }
  }

  private boolean hasAwsCredentials() {
    return awsAccessKey.isPresent()
        && awsSecretKey.isPresent()
        && !awsAccessKey.get().isEmpty()
        && !awsSecretKey.get().isEmpty();
  }

  public Optional<String> getAwsSecretKey() {
    return awsSecretKey;
  }

  public Optional<String> getAwsAccessKey() {
    return awsAccessKey;
  }

  public String getRegion() {
    return region;
  }

  public String getBucket() {
    return bucket;
  }

  public String getEndpoint() {
    return endpoint;
  }
}
