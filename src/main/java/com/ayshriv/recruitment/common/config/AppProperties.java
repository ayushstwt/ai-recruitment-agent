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

        /**
         * Password policy applied when a user password is created or changed.
         */
        private PasswordPolicy passwordPolicy = new PasswordPolicy();
    }

    /**
     * Configurable password strength requirements.
     */
    @Getter
    @Setter
    public static class PasswordPolicy {

        /**
         * Minimum password length.
         */
        private int minLength = 8;

        /**
         * Whether an uppercase letter is required.
         */
        private boolean requireUppercase = true;

        /**
         * Whether a lowercase letter is required.
         */
        private boolean requireLowercase = true;

        /**
         * Whether a digit is required.
         */
        private boolean requireDigit = true;

        /**
         * Whether a non-alphanumeric character is required.
         */
        private boolean requireSpecialCharacter = true;
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