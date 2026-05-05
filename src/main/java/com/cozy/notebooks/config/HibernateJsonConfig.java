package com.cozy.notebooks.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the application's Spring-managed {@link ObjectMapper} into Hibernate
 * so that {@code @JdbcTypeCode(SqlTypes.JSON)} columns serialize/deserialize
 * with the same Jackson configuration the API layer uses.
 *
 * MySQL's native {@code JSON} column is read/written by Hibernate via this
 * mapper for {@code JsonNode}-typed entity fields.
 */
@Configuration
public class HibernateJsonConfig {

    @Bean
    HibernatePropertiesCustomizer jacksonJsonFormatMapperCustomizer(ObjectMapper objectMapper) {
        return props -> props.put(
                "hibernate.type.json_format_mapper",
                new JacksonJsonFormatMapper(objectMapper)
        );
    }
}
