package com.greengrub.donationService.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GreenGrub Donation Service API")
                        .description("""
                                Manages food donation listings on the GreenGrub platform.

                                **Donor flow:** Create a donation (ACTIVE) → update details → cancel if needed (CANCELLED)

                                **Volunteer flow:** Browse ACTIVE donations → claim one (CLAIMED)

                                **Status lifecycle:** `ACTIVE` → `CLAIMED` | `CANCELLED`
                                """)
                        .version("1.1.0")
                        .contact(new Contact()
                                .name("GreenGrub Team")
                                .email("support@greengrub.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8083").description("Local")))
                .tags(List.of(
                        new Tag().name("Donation").description("CRUD and status management for food donation listings")));
    }
}
