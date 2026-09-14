package com.assessly.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "assessly")
public class AppProperties {
    private Deepseek deepseek = new Deepseek();
    private long apiSessionTtlMinutes = 180;

    public Deepseek getDeepseek() {
        return deepseek;
    }

    public void setDeepseek(Deepseek deepseek) {
        this.deepseek = deepseek;
    }

    public long getApiSessionTtlMinutes() {
        return apiSessionTtlMinutes;
    }

    public void setApiSessionTtlMinutes(long apiSessionTtlMinutes) {
        this.apiSessionTtlMinutes = apiSessionTtlMinutes;
    }

    public static class Deepseek {
        private String baseUrl;
        private String model;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}
