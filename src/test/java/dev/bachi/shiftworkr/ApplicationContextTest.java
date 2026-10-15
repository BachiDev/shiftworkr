package dev.bachi.shiftworkr;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Phase 0 gate: proves the Spring context boots (incl. Flyway migrations) on a real
 * PostgreSQL. Catches dependency-mismatch boot failures (e.g. Spring Framework version
 * conflicts) that unit tests miss.
 */
@SpringBootTest
@Testcontainers
class ApplicationContextTest {

  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Test
  void contextLoads() {
  }
}
