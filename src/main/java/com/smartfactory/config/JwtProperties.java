package com.smartfactory.config;

public class JwtProperties {

    private final String secret;

    private final long expirationSeconds;

    public JwtProperties(String secret, long expirationSeconds) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    public String getSecret() {
        return secret;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
