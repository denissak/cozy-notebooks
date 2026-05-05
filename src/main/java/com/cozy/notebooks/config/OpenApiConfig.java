package com.cozy.notebooks.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI cozyOpenAPI() {
        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT token. In MVP, requests are auto-authenticated as the dev user.");

        return new OpenAPI()
                .info(new Info()
                        .title("Cozy Notebooks API")
                        .version("v1")
                        .description("REST API for cozy-notebooks: notebooks, pages, blocks and templates.")
                        .contact(new Contact().name("Cozy Notebooks").email("dev@cozy.local"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearer))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
