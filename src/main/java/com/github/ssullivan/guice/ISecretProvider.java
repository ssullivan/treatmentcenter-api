package com.github.ssullivan.guice;

import com.github.ssullivan.guice.ISecretProvider.DefaultSecretProvider;
import com.google.inject.ImplementedBy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@ImplementedBy(DefaultSecretProvider.class)
public interface ISecretProvider {
  String getSecret(String name) throws IOException;

  InputStream getSecretInputStream(String name) throws IOException;


  class DefaultSecretProvider implements ISecretProvider {

    @Override
    public String getSecret(String name) throws IOException {
      return System.getenv(name);
    }

    @Override
    public InputStream getSecretInputStream(String name) throws IOException {
      String prop = System.getenv(name);
      if (prop != null) {
        return new ByteArrayInputStream(prop.getBytes(Charset.forName("UTF-8")));
      }
      return new ByteArrayInputStream(new byte[]{0});
    }
  }
}
