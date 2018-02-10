package com.github.rupert654.spaceinpathbug;

import javax.servlet.ServletContext;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application implements CommandLineRunner {
  private final ServletContext servletContext;

  public Application(final ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Override
  public void run(final String... args) throws Exception {
    if (servletContext.getResource("/webjars/bootstrap/4.0.0/css/bootstrap.css") == null) {
      throw new RuntimeException("JAR resources not available from servlet context");
    }
  }
}
