package com.reseller.game.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI resellerGameOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Reseller Game API")
                        .version("1.0")
                        .description("REST API documentation for Reseller Game backend. " +
                                "This API manages game rooms, players, and game mechanics for the multiplayer car reselling simulation.")
                        .contact(new Contact()
                                .name("Reseller Game Team")
                                .email("support@resellergame.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server"),
                        new Server()
                                .url("http://localhost:4200")
                                .description("Angular proxy server")
                ));
    }
}
