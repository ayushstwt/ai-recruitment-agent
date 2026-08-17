package com.ayshriv.recruitment.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Application level configuration bound to the {@code app.*} prefix.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * Allowed origins for cross origin requests.
     */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * Security related configuration.
     */
    private Security security = new Security();

    /**
     * Nested security settings.
     */
    @Getter
    @Setter
    public static class Security {

        /**
         * API key authentication settings.
         */
        private ApiKey apiKey = new ApiKey();
    }

    /**
     * API key authentication settings.
     */
    @Getter
    @Setter
    public static class ApiKey {

        /**
         * HTTP header carrying the API key.
         */
        private String headerName = "X-API-KEY";
    }
}