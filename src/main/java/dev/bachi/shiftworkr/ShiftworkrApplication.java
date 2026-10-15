package dev.bachi.shiftworkr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ShiftworkrApplication {
  public static void main(String[] args) {
    SpringApplication.run(ShiftworkrApplication.class, args);
  }
}
