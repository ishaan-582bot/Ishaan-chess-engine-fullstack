package com.chess.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger Configuration
 * 
 * Configures API documentation using SpringDoc OpenAPI.
 * Provides interactive API documentation at /api/swagger-ui.html
 * 
 * @author Chess API Developer
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * Configure OpenAPI documentation
     * @return OpenAPI configuration
     */
    @Bean
    public OpenAPI chessApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chess Engine REST API")
                        .description("RESTful API for chess game management, move execution, and engine analysis")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Chess API Support")
                                .email("support@chessapi.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("Local development server")));
    }
}
