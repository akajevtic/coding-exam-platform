package org.raflab.lms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/*
 * app.security.* iz application.properties
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String jwtSecret,             // HMAC kljuc, najmanje 32 bajta
        Duration jwtTrajanje,         // koliko vazi izdati token
        List<String> corsOrigins) {   // dozvoljeni origin-i za browser klijente
}
