package com.cozy.notebooks;

import com.cozy.notebooks.config.CorsProperties;
import com.cozy.notebooks.security.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Spring Boot's {@link UserDetailsServiceAutoConfiguration} would otherwise
 * create an {@link org.springframework.security.provisioning.InMemoryUserDetailsManager}
 * with a random password and log:
 *
 *   "Using generated security password: <uuid>"
 *
 * This app authenticates via {@link com.cozy.notebooks.security.MockAuthenticationFilter}
 * (and a JWT filter once it ships), so no fallback in-memory user is needed.
 * Excluding the auto-config silences the warning and removes the unused
 * default user from the context.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties({CorsProperties.class, SecurityProperties.class})
public class CozyNotebooksApplication {
    public static void main(String[] args) {
        SpringApplication.run(CozyNotebooksApplication.class, args);
    }
}
