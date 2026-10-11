package com.cargorent.booking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("CarGoRent Booking Service")
                .version("1.0.0")
                .description("Called directly, pass X-User-Id / X-User-Role headers to act as a logged-in user."));
    }
}
