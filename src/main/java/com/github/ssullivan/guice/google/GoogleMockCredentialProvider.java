package com.github.ssullivan.guice.google;

import com.google.api.client.googleapis.testing.auth.oauth2.MockGoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class GoogleMockCredentialProvider implements Provider<MockGoogleCredential> {
    private MockGoogleCredential googleCredential;

    @Inject
    public GoogleMockCredentialProvider(HttpTransport httpTransport) {
        this.googleCredential = new MockGoogleCredential.Builder().setTransport(httpTransport).build();
    }

    @Override
    public MockGoogleCredential get() {
        return this.googleCredential;
    }
}
