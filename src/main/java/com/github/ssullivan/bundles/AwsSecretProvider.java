package com.github.ssullivan.bundles;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.github.ssullivan.guice.AWSCredProviderChain;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;

public class AwsSecretProvider implements ISecretProvider {
  private AWSSecretsManager secretManager;

  @Inject
  public AwsSecretProvider(AWSSecretsManager awsSecretsManager) {
    this.secretManager = awsSecretsManager;
  }

  @Override
  public String getSecret(String name) throws IOException {
    GetSecretValueRequest secretValueRequest = new GetSecretValueRequest()
        .withSecretId(name)
        .withRequestCredentialsProvider(AWSCredProviderChain.getInstance());

    GetSecretValueResult getSecretValueResult =
        secretManager.getSecretValue(secretValueRequest);

    return getSecretValueResult.getSecretString();
  }

  @Override
  public InputStream getSecretInputStream(String name) throws IOException {
    GetSecretValueRequest secretValueRequest = new GetSecretValueRequest()
        .withSecretId(name)
        .withRequestCredentialsProvider(AWSCredProviderChain.getInstance());

    GetSecretValueResult getSecretValueResult =
        secretManager.getSecretValue(secretValueRequest);

    return new ByteArrayInputStream(getSecretValueResult.getSecretBinary().array());
  }
}
