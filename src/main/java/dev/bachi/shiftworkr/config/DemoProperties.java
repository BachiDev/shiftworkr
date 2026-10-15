package dev.bachi.shiftworkr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Demo-profile feature flag (one-click demo logins, seed data, tour banner).
 * Bound from {@code shiftworkr.demo.*}; generates config metadata via
 * spring-boot-configuration-processor so IDEs validate the keys.
 */
@ConfigurationProperties(prefix = "shiftworkr.demo")
public record DemoProperties(@DefaultValue("false") boolean enabled) {
}
