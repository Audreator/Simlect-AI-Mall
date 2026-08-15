package com.simlect.cloud.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simlect.internal")
public class GatewayInternalProperties {

    /** 内部调用令牌；空值表示未配置，此时 /internal/** 一律 401（禁止以空令牌放行） */
    private String token = "";

    private boolean authEnabled = true;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isAuthEnabled() {
        return authEnabled;
    }

    public void setAuthEnabled(boolean authEnabled) {
        this.authEnabled = authEnabled;
    }
}
