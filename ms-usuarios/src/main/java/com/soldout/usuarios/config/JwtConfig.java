package com.soldout.usuarios.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private String secret;
    private long expiracionMs;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiracionMs() {
        return expiracionMs;
    }

    public void setExpiracionMs(long expiracionMs) {
        this.expiracionMs = expiracionMs;
    }

    public String secret() {
        return secret;
    }

    public long expiracionMs() {
        return expiracionMs;
    }
}
