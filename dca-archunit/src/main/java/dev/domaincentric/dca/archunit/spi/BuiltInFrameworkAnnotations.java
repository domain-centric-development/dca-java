package dev.domaincentric.dca.archunit.spi;

import dev.domaincentric.dca.archunit.FrameworkAnnotations;

/**
 * The presets {@code dca-archunit} ships, each as a {@link FrameworkAnnotationsProvider} so that
 * detection and lookup by name treat built-in and third-party presets alike. The values live in
 * {@link FrameworkAnnotations}; this class only adds the detection probe and the priority.
 *
 * <p>Detection probes one class file per framework and never loads it. Priorities: a framework that
 * builds on CDI ({@code quarkus}) outranks the plain Jakarta preset, which is why a Quarkus class
 * path yields {@code quarkus}, not {@code jakarta}. {@code none} is never detected.
 */
public final class BuiltInFrameworkAnnotations {

  private BuiltInFrameworkAnnotations() {}

  static boolean present(ClassLoader loader, String classResource) {
    return loader != null && loader.getResource(classResource) != null;
  }

  /** Spring Framework — detected by {@code org.springframework.context.ApplicationContext}. */
  public static final class Spring implements FrameworkAnnotationsProvider {
    @Override
    public String name() {
      return "spring";
    }

    @Override
    public FrameworkAnnotations annotations() {
      return FrameworkAnnotations.spring();
    }

    @Override
    public boolean detect(ClassLoader loader) {
      return present(loader, "org/springframework/context/ApplicationContext.class");
    }

    @Override
    public int priority() {
      return 10;
    }
  }

  /** Jakarta EE — detected by CDI's {@code jakarta.enterprise.inject.spi.CDI}. */
  public static final class Jakarta implements FrameworkAnnotationsProvider {
    @Override
    public String name() {
      return "jakarta";
    }

    @Override
    public FrameworkAnnotations annotations() {
      return FrameworkAnnotations.jakarta();
    }

    @Override
    public boolean detect(ClassLoader loader) {
      return present(loader, "jakarta/enterprise/inject/spi/CDI.class");
    }

    @Override
    public int priority() {
      return 5;
    }
  }

  /** Quarkus — detected by {@code io.quarkus.runtime.Quarkus}; outranks {@link Jakarta}. */
  public static final class Quarkus implements FrameworkAnnotationsProvider {
    @Override
    public String name() {
      return "quarkus";
    }

    @Override
    public FrameworkAnnotations annotations() {
      return FrameworkAnnotations.quarkus();
    }

    @Override
    public boolean detect(ClassLoader loader) {
      return present(loader, "io/quarkus/runtime/Quarkus.class");
    }

    @Override
    public int priority() {
      return 20;
    }
  }

  /** Micronaut — detected by {@code io.micronaut.context.ApplicationContext}. */
  public static final class Micronaut implements FrameworkAnnotationsProvider {
    @Override
    public String name() {
      return "micronaut";
    }

    @Override
    public FrameworkAnnotations annotations() {
      return FrameworkAnnotations.micronaut();
    }

    @Override
    public boolean detect(ClassLoader loader) {
      return present(loader, "io/micronaut/context/ApplicationContext.class");
    }

    @Override
    public int priority() {
      return 20;
    }
  }

  /** No framework — selectable by name, never detected. */
  public static final class None implements FrameworkAnnotationsProvider {
    @Override
    public String name() {
      return "none";
    }

    @Override
    public FrameworkAnnotations annotations() {
      return FrameworkAnnotations.none();
    }
  }
}
