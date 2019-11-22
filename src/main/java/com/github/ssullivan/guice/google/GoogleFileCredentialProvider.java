package com.github.ssullivan.guice.google;

import com.github.ssullivan.guice.GoogleToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class GoogleFileCredentialProvider implements Provider<GoogleCredential> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleFileCredentialProvider.class);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private GoogleCredential googleCredential;
    private File credentialFile;
    private HttpTransport httpTransport;

    @Inject
    public GoogleFileCredentialProvider(HttpTransport httpTransport, @GoogleToken File credentialFile) {
        this.httpTransport = httpTransport;
        this.credentialFile = credentialFile;
        this.googleCredential = null;
    }

    @Override
    public GoogleCredential get() {
        try {
            this.googleCredential = GoogleCredential.fromStream(new FileInputStream(credentialFile), httpTransport, JSON_FACTORY);
        } catch (FileNotFoundException e) {
            LOGGER.error("Failed to read google credential file because it does not exist: {}", credentialFile, e);
        } catch (IOException e) {
            LOGGER.error("Failed to read google credential file", e);
        }

        return this.googleCredential;
    }
}
