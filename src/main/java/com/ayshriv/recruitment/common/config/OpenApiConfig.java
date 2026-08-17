package com.ayshriv.recruitment.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI documentation customization.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Expose the {@code X-API-KEY} header as the security scheme for all
     * documented endpoints.
     *
     * @return OpenAPI model
     */
    @Bean
    public OpenAPI recruitmentOpenApi() {
        final String schemeName = "X-API-KEY";

        return new OpenAPI()
                .info(new Info()
                        .title("AI Recruitment Agent API")
                        .version("1.0.0")
                        .description("REST API for the AI Recruitment Agent. "
                                + "Authenticate by sending an API key in the "
                                + "X-API-KEY header."))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(schemeName)))
                .addSecurityItem(new SecurityRequirement().addList(schemeName));
    }
}