package dev.bachi.shiftworkr.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import dev.bachi.shiftworkr.ui.views.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Two roles only: ADMIN (ward lead / manager) and NURSE (staff).
 * Lead coverage is a StaffMember flag, not a role. Vaadin 25 wires its
 * navigation access control (@RolesAllowed) through VaadinSecurityConfigurer.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth ->
        auth.requestMatchers("/actuator/health", "/actuator/info").permitAll());
    http.with(VaadinSecurityConfigurer.vaadin(), cfg -> cfg.loginView(LoginView.class));
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
}
