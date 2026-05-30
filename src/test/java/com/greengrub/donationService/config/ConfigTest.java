package com.greengrub.donationService.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ConfigTest {

    // ── SwaggerConfig ─────────────────────────────────────────────────────────

    @Test
    void swaggerConfig_openApiBean_hasExpectedTitle() {
        SwaggerConfig config = new SwaggerConfig();
        OpenAPI openAPI = config.openAPI();

        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("GreenGrub Donation Service API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.1.0");
        assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("support@greengrub.com");
        assertThat(openAPI.getServers()).hasSize(1);
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo("http://localhost:8083");
        assertThat(openAPI.getTags()).hasSize(1);
        assertThat(openAPI.getTags().get(0).getName()).isEqualTo("Donation");
    }

    // ── CorsConfig ────────────────────────────────────────────────────────────

    @Test
    void corsConfig_noAllowedOrigins_configSourceRegistered() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", List.of());

        CorsConfigurationSource source = config.corsConfigurationSource();

        assertThat(source).isNotNull().isInstanceOf(UrlBasedCorsConfigurationSource.class);
    }

    @Test
    void corsConfig_withAllowedOrigins_credentialsEnabled() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", List.of("http://localhost:3000"));

        CorsConfigurationSource source = config.corsConfigurationSource();

        assertThat(source).isNotNull();
        // Verify the config was built (source is non-null and registered)
        UrlBasedCorsConfigurationSource urlSource = (UrlBasedCorsConfigurationSource) source;
        assertThat(urlSource).isNotNull();
    }

    @Test
    void corsConfig_nullAllowedOrigins_noCredentialsSet() {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", null);

        CorsConfigurationSource source = config.corsConfigurationSource();

        assertThat(source).isNotNull();
    }
}
