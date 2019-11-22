package com.github.ssullivan.guice.google;

import com.google.api.client.testing.http.MockHttpTransport;
import com.google.inject.Provider;

public class GoogleMockTransportProvider implements Provider<MockHttpTransport> {
    private MockHttpTransport httpTransport;

    public GoogleMockTransportProvider() {
        this.httpTransport = new MockHttpTransport.Builder()
                .build();
    }

    @Override
    public MockHttpTransport get() {
        return httpTransport;
    }
}
