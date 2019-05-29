package com.github.ssullivan.guice.google;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.inject.Provider;

public class GoogleTransportProvider implements Provider<HttpTransport> {
    private HttpTransport transport;

    public GoogleTransportProvider() {
        this.transport = new NetHttpTransport.Builder().build();
    }

    @Override
    public HttpTransport get() {
        return this.transport;
    }
}
