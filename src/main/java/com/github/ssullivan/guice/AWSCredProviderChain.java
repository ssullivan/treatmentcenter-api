package com.github.ssullivan.guice;

import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;

public class AWSCredProviderChain extends com.amazonaws.auth.AWSCredentialsProviderChain {

  private static final AWSCredentialsProviderChain INSTANCE
      = new AWSCredentialsProviderChain();

  AWSCredProviderChain() {
    super(new DefaultAWSCredentialsProviderChain(),
        InstanceProfileCredentialsProvider.getInstance());
  }

  public static AWSCredentialsProviderChain getInstance() {
    return INSTANCE;
  }
}
