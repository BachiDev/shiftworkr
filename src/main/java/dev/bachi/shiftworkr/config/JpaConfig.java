package dev.bachi.shiftworkr.config;

import java.util.Map;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JPA/Hibernate tuning that cannot live in application.yml without tripping the
 * Boot config validator: {@code hibernate.jdbc.time_zone} requires an underscore
 * (Hibernate reads the key literally; the kebab-case {@code time-zone} variant
 * would be silently ignored). Plan mandate: all timestamps stored as UTC
 * (timestamptz), wall-clock Europe/Berlin only at the edges.
 */
@Configuration
public class JpaConfig {

  @Bean
  HibernatePropertiesCustomizer utcTimeZoneCustomizer() {
    return (Map<String, Object> props) -> props.put("hibernate.jdbc.time_zone", "UTC");
  }
}
