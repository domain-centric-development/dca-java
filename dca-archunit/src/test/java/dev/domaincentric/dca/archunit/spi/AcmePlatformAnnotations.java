package dev.domaincentric.dca.archunit.spi;

import dev.domaincentric.dca.archunit.FrameworkAnnotations;

/**
 * A third-party preset as a library would ship it: one class, one {@code META-INF/services} line,
 * no dependency beyond {@code dca-archunit}. Registered from {@code src/test/resources}.
 */
public final class AcmePlatformAnnotations implements FrameworkAnnotationsProvider {

  public static final String MARKER = "com/acme/platform/Platform.class";

  @Override
  public String name() {
    return "acme";
  }

  @Override
  public FrameworkAnnotations annotations() {
    return FrameworkAnnotations.none()
        .named("acme")
        .withInjectable("com.acme.platform.Managed")
        .withTransactional("com.acme.platform.Atomic")
        .withRestController("com.acme.platform.Endpoint");
  }

  @Override
  public boolean detect(ClassLoader loader) {
    return loader.getResource(MARKER) != null;
  }

  @Override
  public int priority() {
    return 100;
  }
}
