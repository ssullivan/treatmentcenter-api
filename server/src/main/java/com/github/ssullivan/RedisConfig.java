package com.github.ssullivan;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RedisConfig {
    private String host;
    private int port;

    public RedisConfig() {
        this.port = 6379;
    }

    @JsonProperty("host")
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @JsonProperty("port")
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
