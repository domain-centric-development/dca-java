package dev.domaincentric.dca.archunit.spi;

import dev.domaincentric.dca.archunit.FrameworkAnnotations;

/**
 * Supplies one {@link FrameworkAnnotations} preset to {@code dca-archunit} — the extension point
 * through which a framework the library has no preset for is added by a library of its own, without
 * a change to the rules.
 *
 * <p>Implementations are discovered with {@link java.util.ServiceLoader}: a jar lists the class in
 * {@code META-INF/services/dev.domaincentric.dca.archunit.spi.FrameworkAnnotationsProvider} and
 * needs no dependency beyond {@code dca-archunit}. The five built-in presets are providers too.
 *
 * <pre>{@code
 * public final class AcmePlatformAnnotations implements FrameworkAnnotationsProvider {
 *   public String name() { return "acme"; }
 *   public FrameworkAnnotations annotations() {
 *     return FrameworkAnnotations.none()
 *         .named("acme")
 *         .withInjectable("com.acme.platform.Managed")
 *         .withTransactional("com.acme.platform.Atomic")
 *         .withRestController("com.acme.platform.Endpoint");
 *   }
 *   public boolean detect(ClassLoader loader) {
 *     return loader.getResource("com/acme/platform/Platform.class") != null;
 *   }
 * }
 * }</pre>
 *
 * <p>{@link FrameworkAnnotations#detect()} asks every provider whether its framework is on the test
 * class path and takes the one with the highest {@link #priority()}; {@link
 * FrameworkAnnotations#preset(String)} looks a provider up by name. An explicit {@code
 * DcaLayout.withFrameworkAnnotations(...)} always wins over both.
 */
public interface FrameworkAnnotationsProvider {

  /** The preset's name — what the report shows and what {@code dca.framework=} selects. */
  String name();

  /** The preset. */
  FrameworkAnnotations annotations();

  /**
   * Whether the framework this preset describes is present on the given class path. Probe a
   * resource ({@code loader.getResource("…/SomeFrameworkClass.class")}) rather than loading a
   * class, so nothing is initialised. Default: never detected — the preset is selected by name or
   * explicitly.
   */
  default boolean detect(ClassLoader loader) {
    return false;
  }

  /**
   * Tie-break when several providers detect their framework: the highest wins. A framework built on
   * another (Quarkus on CDI) declares a higher priority than the one it builds on. Default {@code
   * 0}.
   */
  default int priority() {
    return 0;
  }
}
