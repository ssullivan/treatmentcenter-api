package com.github.ssullivan.bundles;

import java.io.IOException;
import java.io.InputStream;

public interface ISecretProvider {
  String getSecret(String name) throws IOException;

  InputStream getSecretInputStream(String name) throws IOException;
}
