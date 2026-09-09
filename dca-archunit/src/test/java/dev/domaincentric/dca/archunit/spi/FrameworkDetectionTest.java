package dev.domaincentric.dca.archunit.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.FrameworkAnnotations;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Presets are discovered, not hard-wired: the built-ins and a third-party provider come through
 * {@link java.util.ServiceLoader}; {@link FrameworkAnnotations#detect(ClassLoader)} chooses by what
 * the class path holds. The class paths are simulated by a loader that answers {@code getResource}
 * for a chosen set of class files and otherwise delegates — nothing is loaded.
 */
class FrameworkDetectionTest {

  /** A class path that "contains" exactly the given class files. */
  private static ClassLoader classPathWith(String... classResources) {
    Set<String> present = Set.of(classResources);
    return new ClassLoader(FrameworkDetectionTest.class.getClassLoader()) {
      @Override
      public URL getResource(String name) {
        if (present.contains(name)) {
          try {
            return new URL("file:/simulated/" + name);
          } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
          }
        }
        return name.startsWith("META-INF/services/") ? super.getResource(name) : null;
      }
    };
  }

  @Test
  @DisplayName("the five built-in presets and the test's third-party preset are providers")
  void providersAreDiscoveredThroughServiceLoader() {
    List<String> names =
        FrameworkAnnotations.providers(FrameworkDetectionTest.class.getClassLoader()).stream()
            .map(FrameworkAnnotationsProvider::name)
            .sorted()
            .toList();
    assertEquals(List.of("acme", "jakarta", "micronaut", "none", "quarkus", "spring"), names);
  }

  @Test
  @DisplayName("a Spring class path yields spring (detected)")
  void springIsDetected() {
    FrameworkAnnotations.Detection detection =
        FrameworkAnnotations.detect(
            classPathWith("org/springframework/context/ApplicationContext.class"));
    assertTrue(detection.detected());
    assertEquals("spring", detection.annotations().name());
    assertEquals("spring (detected)", detection.describe());
  }

  @Test
  @DisplayName("Quarkus outranks the Jakarta preset it builds on")
  void quarkusWinsOverJakarta() {
    FrameworkAnnotations.Detection detection =
        FrameworkAnnotations.detect(
            classPathWith(
                "io/quarkus/runtime/Quarkus.class", "jakarta/enterprise/inject/spi/CDI.class"));
    assertEquals("quarkus", detection.annotations().name());
    assertEquals(List.of("quarkus", "jakarta"), detection.candidates());
    assertEquals("quarkus (detected; also jakarta)", detection.describe());
  }

  @Test
  @DisplayName("two frameworks of equal priority decide nothing - spring (default), both reported")
  void equalPriorityIsUndecided() {
    FrameworkAnnotations.Detection detection =
        FrameworkAnnotations.detect(
            classPathWith(
                "io/quarkus/runtime/Quarkus.class",
                "io/micronaut/context/ApplicationContext.class"));
    assertFalse(detection.detected());
    assertEquals("spring", detection.annotations().name());
    assertEquals(List.of("micronaut", "quarkus"), detection.candidates());
    assertEquals("spring (default; undecided: micronaut, quarkus)", detection.describe());
  }

  @Test
  @DisplayName("nothing on the class path falls back to spring (default)")
  void nothingDetectedFallsBackToSpring() {
    FrameworkAnnotations.Detection detection = FrameworkAnnotations.detect(classPathWith());
    assertFalse(detection.detected());
    assertEquals("spring", detection.annotations().name());
    assertEquals("spring (default)", detection.describe());
  }

  @Test
  @DisplayName("a third-party provider is detected and named like a built-in")
  void thirdPartyPresetIsDetected() {
    FrameworkAnnotations.Detection detection =
        FrameworkAnnotations.detect(classPathWith(AcmePlatformAnnotations.MARKER));
    assertTrue(detection.detected());
    assertEquals("acme", detection.annotations().name());
    assertEquals(List.of("com.acme.platform.Atomic"), detection.annotations().transactional());
    assertEquals("acme", FrameworkAnnotations.preset("acme").orElseThrow().name());
  }

  @Test
  @DisplayName("the layout reports how it came by its preset")
  void layoutReportsTheOrigin() {
    DcaLayout detected = DcaLayout.forBasePackage("com.example");
    // this test class path carries only name-only shims, none of the probed marker classes
    assertEquals(
        DcaLayout.FrameworkAnnotationsOrigin.DEFAULT, detected.frameworkAnnotationsOrigin());
    assertEquals("spring (default)", detected.frameworkAnnotationsReport());

    DcaLayout explicit = detected.withFrameworkAnnotations(FrameworkAnnotations.jakarta());
    assertEquals("jakarta (explicit)", explicit.frameworkAnnotationsReport());

    DcaLayout configured = detected.withFrameworkPreset("acme");
    assertEquals("acme (configured)", configured.frameworkAnnotationsReport());
    assertEquals(
        List.of("com.acme.platform.Managed"), configured.frameworkAnnotations().injectable());
    assertTrue(configured.toString().contains("acme (configured)"));

    IllegalArgumentException typo =
        assertThrows(IllegalArgumentException.class, () -> detected.withFrameworkPreset("sprng"));
    assertTrue(
        typo.getMessage().contains("spring"), "names the known presets: " + typo.getMessage());
  }

  @Test
  @DisplayName("dca.framework in the properties selects by name unless the layout is explicit")
  void propertiesSelectAPresetUnlessExplicit() {
    Properties properties = new Properties();
    properties.setProperty("dca.framework", "micronaut");
    DcaLayout layout = DcaLayout.forBasePackage("com.example");

    DcaLayout applied =
        dev.domaincentric.dca.archunit.junit.DcaArchitectureTestAccess.applyConfiguredFramework(
            layout, properties);
    assertEquals("micronaut (configured)", applied.frameworkAnnotationsReport());

    DcaLayout explicit = layout.withFrameworkAnnotations(FrameworkAnnotations.none());
    DcaLayout kept =
        dev.domaincentric.dca.archunit.junit.DcaArchitectureTestAccess.applyConfiguredFramework(
            explicit, properties);
    assertEquals("none (explicit)", kept.frameworkAnnotationsReport());

    DcaLayout untouched =
        dev.domaincentric.dca.archunit.junit.DcaArchitectureTestAccess.applyConfiguredFramework(
            layout, new Properties());
    assertEquals("spring (default)", untouched.frameworkAnnotationsReport());
  }
}
