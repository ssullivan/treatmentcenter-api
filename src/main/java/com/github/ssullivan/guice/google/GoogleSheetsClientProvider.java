package com.github.ssullivan.guice.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class GoogleSheetsClientProvider implements Provider<Sheets> {
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private GoogleCredential googleCredential;
    private HttpTransport httpTransport;

    @Inject
    public GoogleSheetsClientProvider(GoogleCredential googleCredential, HttpTransport httpTransport) {
        this.googleCredential = googleCredential;
        this.httpTransport = httpTransport;
    }

    @Override
    public Sheets get() {
        return new Sheets.Builder(httpTransport, JSON_FACTORY, googleCredential)
                .setApplicationName("SpreadsheetImporter")
                .build();
    }
}
