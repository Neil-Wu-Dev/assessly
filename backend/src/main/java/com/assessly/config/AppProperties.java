package com.assessly.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assessly")
public class AppProperties {
    private long apiSessionTtlMinutes = 180;

    public long getApiSessionTtlMinutes() {
        return apiSessionTtlMinutes;
    }

    public void setApiSessionTtlMinutes(long apiSessionTtlMinutes) {
        this.apiSessionTtlMinutes = apiSessionTtlMinutes;
    }
}
