package dev.couturiaux.wexassessment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class WexassessmentApplication {

  public static void main(String[] args) {
    SpringApplication.run(WexassessmentApplication.class, args);
  }
}
