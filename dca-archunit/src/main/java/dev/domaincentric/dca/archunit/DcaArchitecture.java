package dev.domaincentric.dca.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.SharedKernel;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The classes under test together with the {@link DcaLayout} that describes them, plus the
 * discovery helpers every DCA rule builds on: which packages are bounded contexts, where the shared
 * kernel lives, which annotations a {@code package-info} carries.
 *
 * <p>Create one instance per test run and pass it to every rule — class import and context
 * discovery are cached.
 */
public final class DcaArchitecture {

  private final DcaLayout layout;
  private final JavaClasses classes;
  private Map<String, BoundedContext> boundedContexts;
  private Optional<String> sharedKernelPackage;

  private DcaArchitecture(DcaLayout layout, JavaClasses classes) {
    this.layout = Objects.requireNonNull(layout);
    this.classes = Objects.requireNonNull(classes);
  }

  /**
   * Imports all production classes below the layout's base package (excluding tests, jars and
   * archives) using the class path of the calling test.
   */
  public static DcaArchitecture load(DcaLayout layout) {
    JavaClasses imported =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_ARCHIVES)
            .withImportOption(new DoNotIncludeArchitectureTests())
            .importPackages(layout.basePackage());
    return new DcaArchitecture(layout, imported);
  }

  /** Wraps already imported classes — for tests and custom importers. */
  public static DcaArchitecture of(DcaLayout layout, JavaClasses classes) {
    return new DcaArchitecture(layout, classes);
  }

  public DcaLayout layout() {
    return layout;
  }

  public JavaClasses classes() {
    return classes;
  }

  // ---------------------------------------------------------------------------------------------
  // Bounded-context discovery
  // ---------------------------------------------------------------------------------------------

  /**
   * All packages directly below the base package whose {@code package-info} carries {@link
   * BoundedContext}, keyed by package name, in encounter order.
   */
  public Map<String, BoundedContext> boundedContexts() {
    if (boundedContexts == null) {
      Map<String, BoundedContext> found = new LinkedHashMap<>();
      for (JavaClass javaClass : classes) {
        String root = rootContextPackage(javaClass.getPackageName());
        if (root != null && !found.containsKey(root)) {
          packageAnnotation(root, BoundedContext.class).ifPresent(a -> found.put(root, a));
        }
      }
      boundedContexts = Collections.unmodifiableMap(found);
    }
    return boundedContexts;
  }

  /** Package names of all bounded contexts (no pattern suffix). */
  public List<String> boundedContextPackages() {
    return new ArrayList<>(boundedContexts().keySet());
  }

  /** Bounded-context patterns, e.g. {@code com.acme.shop.cart..}. */
  public String[] boundedContextPatterns() {
    return boundedContexts().keySet().stream().map(p -> p + "..").toArray(String[]::new);
  }

  /** Bounded-context patterns without the given context package. */
  public String[] boundedContextPatternsExcluding(String contextPackage) {
    return boundedContexts().keySet().stream()
        .filter(p -> !p.equals(contextPackage))
        .map(p -> p + "..")
        .toArray(String[]::new);
  }

  /** The package directly below the base package annotated with {@link SharedKernel}. */
  public Optional<String> sharedKernelPackage() {
    if (sharedKernelPackage == null) {
      Optional<String> found = Optional.empty();
      List<String> checked = new ArrayList<>();
      for (JavaClass javaClass : classes) {
        String root = rootContextPackage(javaClass.getPackageName());
        if (root != null && !checked.contains(root)) {
          checked.add(root);
          if (packageAnnotation(root, SharedKernel.class).isPresent()) {
            found = Optional.of(root);
            break;
          }
        }
      }
      sharedKernelPackage = found;
    }
    return sharedKernelPackage;
  }

  /**
   * The direct sub-package of the base package that a fully qualified package belongs to, e.g.
   * {@code com.acme.shop.cart.domain.model → com.acme.shop.cart}; {@code null} for packages outside
   * the base package.
   */
  public String rootContextPackage(String fullPackageName) {
    String prefix = layout.basePackage() + ".";
    if (!fullPackageName.startsWith(prefix)) {
      return null;
    }
    String remainder = fullPackageName.substring(prefix.length());
    int dot = remainder.indexOf('.');
    if (dot > 0) {
      return layout.basePackage() + "." + remainder.substring(0, dot);
    }
    return remainder.isEmpty() ? null : layout.basePackage() + "." + remainder;
  }

  /** Last segment of a context package: {@code com.acme.shop.cart → cart}. */
  public static String simpleContextName(String contextPackage) {
    return contextPackage.substring(contextPackage.lastIndexOf('.') + 1);
  }

  // ---------------------------------------------------------------------------------------------
  // package-info annotations
  // ---------------------------------------------------------------------------------------------

  /** A single annotation from the package's {@code package-info} class, if present. */
  public <T extends Annotation> Optional<T> packageAnnotation(
      String packageName, Class<T> annotationType) {
    return packageInfo(packageName).map(c -> c.getAnnotation(annotationType));
  }

  /** All instances of a repeatable annotation from the package's {@code package-info} class. */
  public <T extends Annotation> List<T> packageAnnotations(
      String packageName, Class<T> annotationType) {
    return packageInfo(packageName)
        .map(c -> List.of(c.getAnnotationsByType(annotationType)))
        .orElse(List.of());
  }

  private Optional<Class<?>> packageInfo(String packageName) {
    try {
      return Optional.of(
          Class.forName(
              packageName + ".package-info",
              false,
              Thread.currentThread().getContextClassLoader()));
    } catch (ClassNotFoundException e) {
      try {
        return Optional.of(Class.forName(packageName + ".package-info"));
      } catch (ClassNotFoundException ignored) {
        return Optional.empty();
      }
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Pattern helpers over the discovered contexts
  // ---------------------------------------------------------------------------------------------

  public String[] contextDomainPatterns() {
    return boundedContexts().keySet().stream().map(layout::domainPattern).toArray(String[]::new);
  }

  public String[] contextDomainModelPatterns() {
    return boundedContexts().keySet().stream()
        .map(layout::domainModelPattern)
        .toArray(String[]::new);
  }

  public String[] contextApplicationPatterns() {
    return boundedContexts().keySet().stream()
        .map(layout::applicationPattern)
        .toArray(String[]::new);
  }

  public String[] contextAdapterPatterns() {
    return boundedContexts().keySet().stream().map(layout::adapterPattern).toArray(String[]::new);
  }

  /** Incoming-adapter patterns of all contexts plus the shared kernel's, if it has one. */
  public String[] allIncomingAdapterPatterns() {
    List<String> patterns = new ArrayList<>();
    boundedContexts().keySet().forEach(p -> patterns.add(layout.incomingAdapterPattern(p)));
    sharedKernelPackage().ifPresent(p -> patterns.add(layout.incomingAdapterPattern(p)));
    return patterns.toArray(String[]::new);
  }

  /** Outgoing-adapter patterns of all contexts plus the shared kernel's, if it has one. */
  public String[] allOutgoingAdapterPatterns() {
    List<String> patterns = new ArrayList<>();
    boundedContexts().keySet().forEach(p -> patterns.add(layout.outgoingAdapterPattern(p)));
    sharedKernelPackage().ifPresent(p -> patterns.add(layout.outgoingAdapterPattern(p)));
    return patterns.toArray(String[]::new);
  }

  /** Domain patterns of all contexts plus the shared kernel domain. */
  public String[] allDomainPatternsWithSharedKernel() {
    List<String> patterns = new ArrayList<>(List.of(contextDomainPatterns()));
    patterns.add(layout.sharedKernelDomainPattern());
    return patterns.toArray(String[]::new);
  }

  /** Domain-model patterns of all contexts plus the shared kernel domain. */
  public String[] allDomainModelPatternsWithSharedKernel() {
    List<String> patterns = new ArrayList<>(List.of(contextDomainModelPatterns()));
    patterns.add(layout.sharedKernelDomainPattern());
    return patterns.toArray(String[]::new);
  }

  /** Classes residing in the global infrastructure implementation packages. */
  public DescribedPredicate<JavaClass> infrastructureImplementation() {
    String prefix = layout.infrastructurePackage() + ".";
    return DescribedPredicate.describe(
        "reside in infrastructure implementation", c -> c.getPackageName().startsWith(prefix));
  }

  /** Excludes compiled test classes (any {@code build/classes/<lang>/test*} directory). */
  public static final class DoNotIncludeArchitectureTests implements ImportOption {
    private static final Pattern TEST_OUTPUT =
        Pattern.compile(".*/build/classes/([^/]+/)?test.*/.*");

    @Override
    public boolean includes(Location location) {
      return !location.matches(TEST_OUTPUT);
    }
  }
}
