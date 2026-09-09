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
import java.util.HashMap;
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

  /**
   * {@code package-info} lookups are reflective and miss far more often than they hit — context
   * discovery walks every ancestor package of every class. Cached per architecture instance,
   * empties included.
   */
  private final Map<String, Optional<Class<?>>> packageInfoCache = new HashMap<>();

  /** Memoised {@link #isContextRoot(String)}, for the same reason. */
  private final Map<String, Boolean> contextRootCache = new HashMap<>();

  private List<String> moduleRoots;

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
   * All packages whose {@code package-info} carries {@link BoundedContext}, keyed by package name,
   * in encounter order. Discovery is by annotation, so a context may sit at any depth below the
   * base package — see {@link #rootContextPackage(String)}.
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

  /**
   * The package annotated with {@link SharedKernel}, at whatever depth below the base package it
   * sits.
   */
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
   * The root package of the bounded context (or shared kernel) that a fully qualified package
   * belongs to — the nearest ancestor at or below the base package whose {@code package-info}
   * carries {@link BoundedContext} or {@link SharedKernel}.
   *
   * <p>Because the declaration is the annotation and not the position in the tree, a context may
   * sit at any depth: {@code com.acme.shop.cart.domain.model → com.acme.shop.cart} when {@code
   * cart} is annotated, and {@code com.acme.shop.sales.order.domain.model →
   * com.acme.shop.sales.order} when {@code sales.order} is. A single-context application may
   * annotate the base package itself.
   *
   * <p>Falls back to the direct sub-package of the base package when no ancestor is annotated, so
   * that a project which has not declared its contexts yet still groups classes the way it used to.
   * Such a package is not a discovered context; whether it owns layers — and is therefore governed
   * — is decided structurally by {@link #moduleRoots()}, not by this fallback.
   *
   * @return the context root package, or {@code null} for packages outside the base package
   */
  public String rootContextPackage(String fullPackageName) {
    if (fullPackageName == null) {
      return null;
    }
    String base = layout.basePackage();
    if (!fullPackageName.equals(base) && !fullPackageName.startsWith(base + ".")) {
      return null;
    }
    for (String candidate = fullPackageName;
        candidate != null;
        candidate = parentPackage(candidate, base)) {
      if (isContextRoot(candidate)) {
        return candidate;
      }
    }
    return firstSegmentBelowBase(fullPackageName);
  }

  private boolean isContextRoot(String packageName) {
    return contextRootCache.computeIfAbsent(
        packageName,
        p ->
            packageAnnotation(p, BoundedContext.class).isPresent()
                || packageAnnotation(p, SharedKernel.class).isPresent());
  }

  /** The parent of {@code packageName}, down to {@code base} inclusive; {@code null} beyond it. */
  private static String parentPackage(String packageName, String base) {
    if (packageName.equals(base)) {
      return null;
    }
    int dot = packageName.lastIndexOf('.');
    return dot < 0 ? null : packageName.substring(0, dot);
  }

  /** The pre-discovery fallback: {@code base.cart.domain.model → base.cart}. */
  private String firstSegmentBelowBase(String fullPackageName) {
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

  /**
   * The identifier of a context package: its name relative to the base package, e.g. {@code
   * com.acme.shop.cart → cart} and {@code com.acme.shop.sales.order → sales.order}.
   *
   * <p>This is the name a context is referenced by in {@code @Upstream(context = ...)}, in the
   * rendered context map, and in rule messages. It deliberately matches the identifier a module
   * system derives from the package tree - the package name trailing the base package - so a module
   * declaration and the context map agree without a mapping layer. For a context that is a direct
   * child of the base package it is the last segment, so nothing changes for a flat layout; grouped
   * contexts keep their group in the name, which is also what makes them unambiguous — two contexts
   * named {@code order} under different groups would otherwise collide.
   */
  public String contextName(String contextPackage) {
    String base = layout.basePackage();
    if (contextPackage.equals(base)) {
      return simpleContextName(base);
    }
    return contextPackage.startsWith(base + ".")
        ? contextPackage.substring(base.length() + 1)
        : simpleContextName(contextPackage);
  }

  /**
   * Last segment of a package: {@code com.acme.shop.cart → cart}.
   *
   * @deprecated a context identifier is its name relative to the base package — use {@link
   *     #contextName(String)}, which is unambiguous for grouped contexts. This method stays for
   *     callers that really do want the last segment.
   */
  @Deprecated(since = "0.2.0")
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
    return packageInfoCache.computeIfAbsent(packageName, DcaArchitecture::loadPackageInfo);
  }

  private static Optional<Class<?>> loadPackageInfo(String packageName) {
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
  // Module discovery — structural, unlike context discovery
  // ---------------------------------------------------------------------------------------------

  /**
   * Every package that owns a DCA layer, in encounter order — the roots the layer rules apply to.
   *
   * <p><b>Why this is not the same as {@link #boundedContexts()}.</b> Being a bounded context is a
   * strategic statement: it is declared with {@code @BoundedContext} and it decides isolation, the
   * context map, upstream relationships. Owning a {@code domain}/{@code application}/{@code
   * adapter} layer is a structural fact, and the layer rules — the domain knows no infrastructure,
   * transactions are an application concern, a {@code Command} lives in {@code application} — apply
   * to it either way. A module that is deliberately <em>not</em> a bounded context (an operational
   * backoffice reading other contexts' data, say) still follows DCA layering and must still be
   * governed.
   *
   * <p>A root is the <b>shortest</b> package prefix at or below the base package whose remainder
   * starts with a layer segment. Shortest wins so that an adapter's own {@code domain} package — an
   * outgoing adapter mapping to a foreign model — stays inside its module instead of becoming a
   * root of its own: for {@code base.cart.adapter.outgoing.domain.Foo} the root is {@code
   * base.cart}, not {@code base.cart.adapter.outgoing}.
   *
   * <p>Because the test is structural, a module is found at any depth and without any annotation —
   * which is what keeps a grouped or nested layout governed. The isolation rules select over module
   * roots as well ({@link #isolatedModuleRoots()}), so a module that declares nothing can neither
   * reach into a neighbour's internals nor have its own internals reached into. Declaring a module
   * a bounded context decides its place on the context map, nothing more.
   */
  public List<String> moduleRoots() {
    if (moduleRoots == null) {
      String base = layout.basePackage();
      java.util.Set<String> layers = layerSegments();
      java.util.LinkedHashSet<String> roots = new java.util.LinkedHashSet<>();
      for (JavaClass javaClass : classes) {
        String root = moduleRootOf(javaClass.getPackageName(), base, layers);
        if (root != null) {
          roots.add(root);
        }
      }
      moduleRoots = List.copyOf(roots);
    }
    return moduleRoots;
  }

  /**
   * The module roots the isolation rules govern: every module root except the shared kernel. The
   * shared kernel is a module root too (it may own {@code domain}, {@code application} and {@code
   * adapter} packages), but everyone may depend on it, and what <em>it</em> may depend on is {@code
   * DCA-STR-002}'s business.
   */
  public List<String> isolatedModuleRoots() {
    Optional<String> sharedKernel = sharedKernelPackage();
    return moduleRoots().stream().filter(root -> !sharedKernel.equals(Optional.of(root))).toList();
  }

  /**
   * Package patterns of every isolated module root except the given one — {@code root..} each.
   * Built from {@link #isolatedModuleRoots()}, so an undeclared module is a forbidden target like
   * any other, not only a governed source.
   */
  public String[] moduleRootPatternsExcluding(String moduleRoot) {
    return isolatedModuleRoots().stream()
        .filter(root -> !root.equals(moduleRoot))
        .map(root -> root + "..")
        .toArray(String[]::new);
  }

  /**
   * The published packages of every isolated module root except the given one: {@code root.api..}
   * (synchronous, in-process) and {@code root.events..} (asynchronous), with the segment names
   * taken from {@link DcaLayout#publishedSubpackages()}. These are DCA's in-process contract
   * convention — package names, a convention of the architecture and not of any framework — and the
   * only part of a foreign module an adapter may depend on.
   */
  public String[] publishedPackagePatternsExcluding(String moduleRoot) {
    return isolatedModuleRoots().stream()
        .filter(root -> !root.equals(moduleRoot))
        .flatMap(root -> layout.publishedSubpackages().stream().map(sub -> root + "." + sub + ".."))
        .toArray(String[]::new);
  }

  /**
   * The layer sub-package names of this layout: {@code domain}, {@code application}, {@code
   * adapter}.
   */
  public java.util.Set<String> layerSegments() {
    return java.util.Set.of(
        layout.domainSubpackage(), layout.applicationSubpackage(), layout.adapterSubpackage());
  }

  /**
   * The module root of a package, or {@code null} when the package carries no layer segment — the
   * global infrastructure package, for instance, or a plain support package.
   */
  public String moduleRootOf(String packageName) {
    return moduleRootOf(packageName, layout.basePackage(), layerSegments());
  }

  private static String moduleRootOf(
      String packageName, String base, java.util.Set<String> layers) {
    if (packageName == null || !packageName.startsWith(base + ".")) {
      return null;
    }
    String[] segments = packageName.substring(base.length() + 1).split("\\.");
    StringBuilder root = new StringBuilder(base);
    for (String segment : segments) {
      if (layers.contains(segment)) {
        return root.toString();
      }
      root.append('.').append(segment);
    }
    return null;
  }

  // ---------------------------------------------------------------------------------------------
  // Pattern helpers: context* over declared bounded contexts, all* over module roots
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

  /**
   * Application-layer patterns of every module root — the discovery equivalent of the former {@code
   * base.*.application..} wildcard, which matched a direct child of the base package and nothing
   * else. Built from {@link #moduleRoots()}, so it holds at any depth.
   */
  public String[] allApplicationPatterns() {
    return moduleRoots().stream().map(layout::applicationPattern).toArray(String[]::new);
  }

  /** Adapter patterns of every module root. */
  public String[] allAdapterPatterns() {
    return moduleRoots().stream().map(layout::adapterPattern).toArray(String[]::new);
  }

  /** Shared-output-port patterns ({@code application.shared}) of every module root. */
  public String[] allSharedOutputPortPatterns() {
    return moduleRoots().stream().map(layout::sharedOutputPortPattern).toArray(String[]::new);
  }

  /** Domain-layer patterns of every module root. */
  public String[] allDomainPatterns() {
    return moduleRoots().stream().map(layout::domainPattern).toArray(String[]::new);
  }

  /** Domain-model patterns of every module root. */
  public String[] allDomainModelPatterns() {
    return moduleRoots().stream().map(layout::domainModelPattern).toArray(String[]::new);
  }

  /** Incoming-adapter patterns of every module root. */
  public String[] allIncomingAdapterPatterns() {
    return moduleRoots().stream().map(layout::incomingAdapterPattern).toArray(String[]::new);
  }

  /** Outgoing-adapter patterns of every module root. */
  public String[] allOutgoingAdapterPatterns() {
    return moduleRoots().stream().map(layout::outgoingAdapterPattern).toArray(String[]::new);
  }

  /**
   * The infrastructure packages of this architecture: the global one ({@code base.infrastructure})
   * and every isolated module's own ({@code base.cart.infrastructure}), each without pattern
   * suffix. A module's infrastructure is not a layer — it does not make the module a root — but it
   * is an implementation detail like the global one, and the rules that keep implementation details
   * out of the inner layers treat both alike.
   *
   * <p>The shared kernel's {@code infrastructure} package is deliberately not in this list. The
   * shared kernel is the one package everyone may depend on; what it keeps under {@code
   * infrastructure} — a project-wide lifecycle annotation, say — is shared support, not a detail of
   * one module that another module's adapter would be reaching into.
   */
  public List<String> infrastructurePackages() {
    List<String> packages = new ArrayList<>();
    packages.add(layout.infrastructurePackage());
    for (String root : isolatedModuleRoots()) {
      String modulePackage = layout.infrastructurePackage(root);
      if (!packages.contains(modulePackage)) {
        packages.add(modulePackage);
      }
    }
    return packages;
  }

  /** {@link #infrastructurePackages()} as patterns, {@code package..} each. */
  public String[] allInfrastructurePatterns() {
    return infrastructurePackages().stream().map(p -> p + "..").toArray(String[]::new);
  }

  /**
   * Classes residing in an infrastructure package — the package itself or any sub-package, with an
   * exact segment boundary: {@code base.infrastructure.Wiring} counts, {@code
   * base.infrastructurex.Other} does not.
   */
  public DescribedPredicate<JavaClass> infrastructureImplementation() {
    List<String> packages = infrastructurePackages();
    return DescribedPredicate.describe(
        "reside in infrastructure implementation",
        c -> packages.stream().anyMatch(p -> inPackageTree(c.getPackageName(), p)));
  }

  /**
   * Every package at or below the base package that an imported class lives in, together with all
   * its ancestors down to the base package, in encounter order. This is the set of packages that
   * may carry a {@code package-info} declaration.
   */
  public List<String> packagesBelowBase() {
    String base = layout.basePackage();
    java.util.LinkedHashSet<String> packages = new java.util.LinkedHashSet<>();
    for (JavaClass javaClass : classes) {
      String pkg = javaClass.getPackageName();
      if (!inPackageTree(pkg, base)) {
        continue;
      }
      for (String candidate = pkg; candidate != null; candidate = parentPackage(candidate, base)) {
        packages.add(candidate);
      }
    }
    return List.copyOf(packages);
  }

  /** True when {@code packageName} is {@code root} itself or a sub-package of it. */
  public static boolean inPackageTree(String packageName, String root) {
    return packageName.equals(root) || packageName.startsWith(root + ".");
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
