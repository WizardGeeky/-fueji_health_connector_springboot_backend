package com.healthconnector.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger / OpenAPI 3 configuration with JWT bearer auth scheme.
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Smart Healthcare Connector Platform API")
                        .description("""
                                Production-grade Prior Authorization Platform.
                                
                                **Roles**: SUPER_ADMIN | PROVIDER | PAYER
                                
                                **Authentication**: Use `POST /api/auth/login` to obtain a JWT Bearer token.
                                Click **Authorize** and enter: `Bearer <your_token>`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("HealthConnector Inc.")
                                .email("support@healthconnector.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://healthconnector.com/terms")))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort).description("Local Development"),
                        new Server().url("https://api.healthconnector.com").description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT Bearer token obtained from POST /api/auth/login")));
    }
}
