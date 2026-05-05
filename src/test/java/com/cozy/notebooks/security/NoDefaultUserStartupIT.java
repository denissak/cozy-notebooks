package com.cozy.notebooks.security;

import com.cozy.notebooks.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against the regression where Spring Boot's
 * {@code UserDetailsServiceAutoConfiguration} kicks in and logs:
 *
 *   "Using generated security password: <uuid>"
 *
 * That message is printed from
 * {@link org.springframework.boot.autoconfigure.security.SecurityProperties.User#getPassword()}
 * the first time the auto-configured {@link InMemoryUserDetailsManager} bean
 * is initialized. If neither the {@link InMemoryUserDetailsManager} nor any
 * {@link UserDetailsService} is present, the message cannot be produced.
 *
 * The application authenticates via {@link MockAuthenticationFilter} (and a
 * JWT filter once it ships), so there must be no fallback in-memory user.
 */
class NoDefaultUserStartupIT extends AbstractIntegrationTest {

    @Autowired
    ApplicationContext context;

    @Test
    void noInMemoryUserDetailsManager_isRegistered() {
        assertThat(context.getBeansOfType(InMemoryUserDetailsManager.class))
                .as("Spring Boot must NOT auto-create an InMemoryUserDetailsManager; "
                        + "that is the source of the 'Using generated security password' warning.")
                .isEmpty();
    }

    @Test
    void noUserDetailsService_isRegistered() {
        assertThat(context.getBeansOfType(UserDetailsService.class))
                .as("No UserDetailsService should be present: auth happens via "
                        + "MockAuthenticationFilter (and JWT later), not in-memory users.")
                .isEmpty();
    }
}
