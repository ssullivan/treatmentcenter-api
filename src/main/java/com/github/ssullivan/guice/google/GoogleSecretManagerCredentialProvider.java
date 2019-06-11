package com.github.ssullivan.guice.google;

import com.github.ssullivan.guice.ISecretProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a GoogleCredential load from the AWS SecretManager
 */
public class GoogleSecretManagerCredentialProvider implements Provider<GoogleCredential> {
  private static final Logger LOGGER = LoggerFactory.getLogger(GoogleSecretManagerCredentialProvider.class);
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();


  private static final String GOOGLE_CREDENTIAL_SECRET_NAME = "Google/SheetsGoogleCredential.json";

  private ISecretProvider secretProvider;
  private HttpTransport httpTransport;

  @Inject
  public GoogleSecretManagerCredentialProvider(ISecretProvider secretProvider, HttpTransport httpTransport) {
    this.secretProvider = secretProvider;
    this.httpTransport = httpTransport;
  }

  @Override
  public GoogleCredential get() {
    try {
      return GoogleCredential.fromStream(secretProvider.getSecretInputStream(GOOGLE_CREDENTIAL_SECRET_NAME), httpTransport, JSON_FACTORY);
    } catch (FileNotFoundException e) {
      LOGGER.error("Failed to read google credential file because it does not exist: {}", GOOGLE_CREDENTIAL_SECRET_NAME, e);
    } catch (IOException e) {
      LOGGER.error("Failed to read google credential file", e);
    }
    return null;
  }
}
