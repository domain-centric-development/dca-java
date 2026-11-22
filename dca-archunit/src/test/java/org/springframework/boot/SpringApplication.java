package org.springframework.boot;

/** Test shim — mirrors the framework's entry by name only. */
public final class SpringApplication {
  private SpringApplication() {}

  public static Object run(Class<?> primarySource, String... args) {
    return primarySource;
  }
}
