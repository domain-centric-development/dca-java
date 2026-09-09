package dev.domaincentric.dca.archunit;

import dev.domaincentric.dca.archunit.spi.FrameworkAnnotationsProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.WeakHashMap;

/**
 * The framework annotations the DCA rules refer to, grouped by <em>role</em>. The rule library has
 * no compile-time dependency on any framework: annotations are matched by fully qualified name, so
 * the same rules govern a Spring, Jakarta EE, Quarkus, Micronaut or hand-wired code base.
 *
 * <p>Every role is a list of fully qualified annotation names — a framework may offer several
 * annotations for one role ({@code @Service} and {@code @Component} are both injectable
 * stereotypes; a Spring Boot project may mix Spring's and Jakarta's {@code @Transactional}). A rule
 * that <em>forbids</em> a role treats every listed annotation as forbidden; a rule that
 * <em>requires</em> a role accepts any of them. An empty role means the framework has no such
 * annotation — rules that need it then select nothing and pass, as documented in their {@code
 * checks} text.
 *
 * <p>Start from a preset — {@link #spring()}, {@link #jakarta()}, {@link #quarkus()}, {@link
 * #micronaut()} or {@link #none()} — and adjust single roles with the {@code with*} methods. {@link
 * DcaLayout#forBasePackage(String)} chooses the preset itself through {@link #detect()}: the
 * framework on the test class path decides, Spring when nothing is found, and the report says which
 * and why. A library may contribute a preset of its own through {@link
 * FrameworkAnnotationsProvider}; {@link #preset(String)} finds any preset by name.
 *
 * <pre>{@code
 * DcaLayout.forBasePackage("com.acme.billing")
 *     .withFrameworkAnnotations(
 *         FrameworkAnnotations.jakarta().withRestController("com.acme.platform.Endpoint"));
 * }</pre>
 *
 * <p>The roles:
 *
 * <ul>
 *   <li>{@link #injectable()} — stereotypes that make a class a container-managed component (Spring
 *       {@code @Service}/{@code @Component}, CDI {@code @ApplicationScoped}, {@code @Singleton}).
 *       Forbidden on domain models, domain events, domain services, factories and specifications;
 *       required on use cases.
 *   <li>{@link #webController()} — the stereotype of a server-rendering (MVC) controller. Selects
 *       the classes whose name must end with the configured controller suffix.
 *   <li>{@link #restController()} — the stereotype of a REST endpoint class. Selects the classes
 *       whose name must end with the configured REST-controller suffix.
 *   <li>{@link #transactional()} — declarative transaction demarcation on a class or method.
 *       Belongs to the application layer; a use case that publishes domain events needs it or an
 *       explicit {@code TransactionBoundary}.
 *   <li>{@link #eventListener()} — an in-process event listener. Forbidden on domain events.
 *   <li>{@link #moduleDeclaration()} — a module system's declaration on {@code package-info}
 *       (Spring Modulith's {@code @ApplicationModule}). Read reflectively for its {@code
 *       allowedDependencies}; empty when the project uses no module system.
 *   <li>{@link #publishedInterface()} — a module system's declaration of a published package
 *       (Spring Modulith's {@code @NamedInterface}). The context-map renderer treats a channel
 *       package as published only when it carries it; empty means class presence stands alone.
 *   <li>{@link #persistenceEntity()} — an ORM's mapping annotations for a persistent class (JPA
 *       {@code @Entity}/{@code @Table}). Forbidden on domain models: the domain is mapped in an
 *       outgoing adapter, not annotated.
 * </ul>
 *
 * @param name the preset's name, shown in the test report so a reader knows which vocabulary the
 *     rules resolved ({@code spring}, {@code jakarta}, …, or whatever {@link #named(String)} set)
 * @param injectable stereotypes of container-managed components
 * @param webController stereotype of server-rendering controllers
 * @param restController stereotype of REST endpoint classes
 * @param transactional declarative transaction demarcation
 * @param eventListener in-process event listener
 * @param moduleDeclaration module declaration on {@code package-info}
 * @param publishedInterface published-package declaration on {@code package-info}
 * @param persistenceEntity ORM mapping annotations of a persistent class
 */
public record FrameworkAnnotations(
    String name,
    List<String> injectable,
    List<String> webController,
    List<String> restController,
    List<String> transactional,
    List<String> eventListener,
    List<String> moduleDeclaration,
    List<String> publishedInterface,
    List<String> persistenceEntity) {

  public FrameworkAnnotations {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    injectable = role(injectable, "injectable");
    webController = role(webController, "webController");
    restController = role(restController, "restController");
    transactional = role(transactional, "transactional");
    eventListener = role(eventListener, "eventListener");
    moduleDeclaration = role(moduleDeclaration, "moduleDeclaration");
    publishedInterface = role(publishedInterface, "publishedInterface");
    persistenceEntity = role(persistenceEntity, "persistenceEntity");
  }

  private static List<String> role(List<String> names, String role) {
    Objects.requireNonNull(names, role);
    for (String fqn : names) {
      if (fqn == null || fqn.isBlank()) {
        throw new IllegalArgumentException(role + " must not contain a blank annotation name");
      }
    }
    return List.copyOf(names);
  }

  // ---------------------------------------------------------------------------------------------
  // Presets
  // ---------------------------------------------------------------------------------------------

  /**
   * Spring Framework, Spring Modulith and JPA — the default of {@link DcaLayout} and the vocabulary
   * of the reference implementation. The transactional role holds Spring's own annotation and JTA's
   * {@code jakarta.transaction.Transactional}, which Spring honours as well once the API is on the
   * class path — a use case may carry either.
   */
  public static FrameworkAnnotations spring() {
    return new FrameworkAnnotations(
        "spring",
        List.of(
            "org.springframework.stereotype.Service", "org.springframework.stereotype.Component"),
        List.of("org.springframework.stereotype.Controller"),
        List.of("org.springframework.web.bind.annotation.RestController"),
        List.of(
            "org.springframework.transaction.annotation.Transactional",
            "jakarta.transaction.Transactional"),
        List.of("org.springframework.context.event.EventListener"),
        List.of("org.springframework.modulith.ApplicationModule"),
        List.of("org.springframework.modulith.NamedInterface"),
        List.of("jakarta.persistence.Entity", "jakarta.persistence.Table"));
  }

  /**
   * Jakarta EE: CDI scopes as injectable stereotypes, Jakarta MVC controllers, JAX-RS resources,
   * JTA {@code @Transactional}, CDI {@code @Observes}, JPA entities. No module system.
   */
  public static FrameworkAnnotations jakarta() {
    return new FrameworkAnnotations(
        "jakarta",
        List.of(
            "jakarta.enterprise.context.ApplicationScoped",
            "jakarta.enterprise.context.Dependent",
            "jakarta.enterprise.context.RequestScoped",
            "jakarta.inject.Singleton"),
        List.of("jakarta.mvc.Controller"),
        List.of("jakarta.ws.rs.Path"),
        List.of("jakarta.transaction.Transactional"),
        List.of("jakarta.enterprise.event.Observes"),
        List.of(),
        List.of(),
        List.of("jakarta.persistence.Entity", "jakarta.persistence.Table"));
  }

  /**
   * Quarkus: the Jakarta preset (Arc is CDI, RESTEasy is JAX-RS, Narayana is JTA) plus Quarkus's
   * own event-bus listener. Quarkus has no server-rendering controller stereotype — templates are
   * served from JAX-RS resources — so the web-controller role is empty.
   */
  public static FrameworkAnnotations quarkus() {
    return new FrameworkAnnotations(
        "quarkus",
        List.of(
            "jakarta.enterprise.context.ApplicationScoped",
            "jakarta.enterprise.context.Dependent",
            "jakarta.enterprise.context.RequestScoped",
            "jakarta.inject.Singleton"),
        List.of(),
        List.of("jakarta.ws.rs.Path"),
        List.of("jakarta.transaction.Transactional"),
        List.of("jakarta.enterprise.event.Observes", "io.quarkus.vertx.ConsumeEvent"),
        List.of(),
        List.of(),
        List.of("jakarta.persistence.Entity", "jakarta.persistence.Table"));
  }

  /**
   * Micronaut: {@code jakarta.inject} scopes as injectable stereotypes, Micronaut's
   * {@code @Controller} as the REST endpoint stereotype (it serves JSON by default; a project that
   * renders views from it sets the REST-controller suffix accordingly), Micronaut's and JTA's
   * {@code @Transactional}, Micronaut's {@code @EventListener}, JPA and Micronaut Data entities.
   */
  public static FrameworkAnnotations micronaut() {
    return new FrameworkAnnotations(
        "micronaut",
        List.of(
            "jakarta.inject.Singleton",
            "io.micronaut.context.annotation.Prototype",
            "io.micronaut.context.annotation.Bean"),
        List.of(),
        List.of("io.micronaut.http.annotation.Controller"),
        List.of(
            "io.micronaut.transaction.annotation.Transactional",
            "jakarta.transaction.Transactional"),
        List.of("io.micronaut.runtime.event.annotation.EventListener"),
        List.of(),
        List.of(),
        List.of(
            "jakarta.persistence.Entity",
            "jakarta.persistence.Table",
            "io.micronaut.data.annotation.MappedEntity"));
  }

  /**
   * No framework annotations at all — a hand-wired application, or a framework the presets do not
   * know yet (build it up with the {@code with*} methods). Rules that forbid a role find nothing to
   * forbid; rules that require one select nothing and pass; the rules about transactions see only
   * explicit {@code TransactionBoundary} calls.
   */
  public static FrameworkAnnotations none() {
    return new FrameworkAnnotations(
        "none", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
        List.of());
  }

  // ---------------------------------------------------------------------------------------------
  // Discovery: presets contributed through the SPI, and the one the class path asks for
  // ---------------------------------------------------------------------------------------------

  /**
   * What {@link #detect()} found: the chosen preset, whether a framework was actually detected (as
   * opposed to falling back to {@link #spring()}), and the names of every provider that recognised
   * the class path, winner first.
   */
  public record Detection(
      FrameworkAnnotations annotations, boolean detected, List<String> candidates) {
    public Detection {
      Objects.requireNonNull(annotations, "annotations");
      candidates = List.copyOf(candidates);
    }

    /**
     * {@code quarkus (detected)}, {@code quarkus (detected; also jakarta)}, {@code spring
     * (default)}, {@code spring (default; undecided: micronaut, quarkus)} when two frameworks of
     * equal priority were found and none was chosen.
     */
    public String describe() {
      if (!detected) {
        return candidates.isEmpty()
            ? annotations.name() + " (default)"
            : annotations.name() + " (default; undecided: " + String.join(", ", candidates) + ")";
      }
      if (candidates.size() > 1) {
        return annotations.name()
            + " (detected; also "
            + String.join(", ", candidates.subList(1, candidates.size()))
            + ")";
      }
      return annotations.name() + " (detected)";
    }
  }

  private static final Map<ClassLoader, Detection> DETECTIONS = new WeakHashMap<>();

  /** {@link #detect(ClassLoader)} for the thread's context class loader (or this class's). */
  public static Detection detect() {
    return detect(defaultLoader());
  }

  /**
   * Asks every {@link FrameworkAnnotationsProvider} on the given class path whether its framework
   * is present and returns the preset of the one with the highest priority — {@link #spring()} with
   * {@code detected == false} when none answers, and likewise when two providers of <em>equal</em>
   * priority both answer (a mixed class path: nothing is chosen, the candidates are reported, the
   * project names its preset). Cached per class loader. An explicit {@link
   * DcaLayout#withFrameworkAnnotations(FrameworkAnnotations)} is never overridden by this.
   */
  public static Detection detect(ClassLoader loader) {
    ClassLoader effective = loader == null ? defaultLoader() : loader;
    synchronized (DETECTIONS) {
      Detection cached = DETECTIONS.get(effective);
      if (cached != null) {
        return cached;
      }
    }
    List<FrameworkAnnotationsProvider> matching = new ArrayList<>();
    for (FrameworkAnnotationsProvider provider : providers(effective)) {
      if (provider.detect(effective)) {
        matching.add(provider);
      }
    }
    matching.sort(
        Comparator.comparingInt(FrameworkAnnotationsProvider::priority)
            .reversed()
            .thenComparing(FrameworkAnnotationsProvider::name));
    List<String> names = matching.stream().map(FrameworkAnnotationsProvider::name).toList();
    boolean undecided =
        matching.size() > 1 && matching.get(0).priority() == matching.get(1).priority();
    Detection detection =
        matching.isEmpty() || undecided
            ? new Detection(spring(), false, names)
            : new Detection(matching.get(0).annotations(), true, names);
    synchronized (DETECTIONS) {
      DETECTIONS.put(effective, detection);
    }
    return detection;
  }

  /**
   * The preset a provider registers under the given name — built-in ({@code spring}, {@code
   * jakarta}, {@code quarkus}, {@code micronaut}, {@code none}) or contributed by a library on the
   * class path; empty when no provider has that name.
   */
  public static Optional<FrameworkAnnotations> preset(String name) {
    for (FrameworkAnnotationsProvider provider : providers(defaultLoader())) {
      if (provider.name().equals(name)) {
        return Optional.of(provider.annotations());
      }
    }
    return Optional.empty();
  }

  /** Every provider {@link ServiceLoader} finds on the given class path, built-ins included. */
  public static List<FrameworkAnnotationsProvider> providers(ClassLoader loader) {
    List<FrameworkAnnotationsProvider> found = new ArrayList<>();
    ServiceLoader.load(FrameworkAnnotationsProvider.class, loader).forEach(found::add);
    if (found.isEmpty() && loader != FrameworkAnnotations.class.getClassLoader()) {
      // A test class loader that does not delegate to ours still gets the built-ins.
      ServiceLoader.load(
              FrameworkAnnotationsProvider.class, FrameworkAnnotations.class.getClassLoader())
          .forEach(found::add);
    }
    return found;
  }

  private static ClassLoader defaultLoader() {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    return loader == null ? FrameworkAnnotations.class.getClassLoader() : loader;
  }

  // ---------------------------------------------------------------------------------------------
  // Adjusting a preset
  // ---------------------------------------------------------------------------------------------

  /**
   * This set under another name — for the report, when a preset was adjusted beyond recognition.
   */
  public FrameworkAnnotations named(String value) {
    return new FrameworkAnnotations(
        value,
        injectable,
        webController,
        restController,
        transactional,
        eventListener,
        moduleDeclaration,
        publishedInterface,
        persistenceEntity);
  }

  public FrameworkAnnotations withInjectable(String... annotationNames) {
    return new FrameworkAnnotations(
        name,
        List.of(annotationNames),
        webController,
        restController,
        transactional,
        eventListener,
        moduleDeclaration,
        publishedInterface,
        persistenceEntity);
  }

  public FrameworkAnnotations withWebController(String... annotationNames) {
    return new FrameworkAnnotations(
        name,
        injectable,
        List.of(annotationNames),
        restController,
        transactional,
        eventListener,
        moduleDeclaration,
        publishedInterface,
        persistenceEntity);
  }

  public FrameworkAnnotations withRestController(String... annotationNames) {
    return new FrameworkAnnotations(
        name,
        injectable,
        webController,
        List.of(annotationNames),
        transactional,
        eventListener,
        moduleDeclaration,
        publishedInterface,
        persistenceEntity);
  }

  public FrameworkAnnotations withTransactional(String... annotationNames) {
    return new FrameworkAnnotations(
        name,
        injectable,
        webController,
        restController,
        List.of(annotationNames),
        eventListener,
        moduleDeclaration,
        publishedInterface,
        persistenceEntity);
  }

  public FrameworkAnnotations withEventListener(String... annotationNames) {
    return new FrameworkAnnotations(
        name,
        injectable,
        webController,
        restController,
        transactional,
        List.of(annotationNames),
        moduleDeclaration,
        publishedInterface,
        persistenceEntity);
  }

  public FrameworkAnnotations withModuleDeclaration(String... annotationNames) {
    return new FrameworkAnnotations(
        name,
        injectable,
        webController,
        restController,
        transactional,
        eventListener,
        List.of(annotationNames),
        publishedInterface,
        persistenceEntity);
  }

  public FrameworkAnnotations withPublishedInterface(String... annotationNames) {
    return new FrameworkAnnotations(
        name,
        injectable,
        webController,
        restController,
        transactional,
        eventListener,
        moduleDeclaration,
        List.of(annotationNames),
        persistenceEntity);
  }

  public FrameworkAnnotations withPersistenceEntity(String... annotationNames) {
    return new FrameworkAnnotations(
        name,
        injectable,
        webController,
        restController,
        transactional,
        eventListener,
        moduleDeclaration,
        publishedInterface,
        List.of(annotationNames));
  }

  // ---------------------------------------------------------------------------------------------
  // Queries
  // ---------------------------------------------------------------------------------------------

  /** Whether a module-declaration annotation is configured. */
  public boolean hasModuleDeclaration() {
    return !moduleDeclaration.isEmpty();
  }

  /**
   * The annotations of a role as they appear in a message: {@code @Transactional} for one,
   * {@code @Transactional/@Transactional} collapsed to the distinct simple names, or the given
   * fallback when the role is empty.
   */
  public static String describe(List<String> role, String whenEmpty) {
    if (role.isEmpty()) {
      return whenEmpty;
    }
    List<String> names = new ArrayList<>();
    for (String fqn : role) {
      String simple = "@" + fqn.substring(fqn.lastIndexOf('.') + 1);
      if (!names.contains(simple)) {
        names.add(simple);
      }
    }
    return String.join("/", names);
  }

  @Override
  public String toString() {
    return name;
  }

  // ---------------------------------------------------------------------------------------------
  // Compatibility with the 0.3 shape (Spring stereotype names as accessors)
  // ---------------------------------------------------------------------------------------------

  /**
   * The 0.3 constructor shape, one annotation per Spring stereotype. {@code service} and {@code
   * component} become the injectable role; {@code applicationModule} may be {@code null}. The two
   * roles 0.3 hard-coded keep their 0.3 values: JPA's {@code @Entity}/{@code @Table} as persistence
   * entities and Spring Modulith's {@code @NamedInterface} as the published-interface declaration,
   * so a context map rendered through this factory does not change.
   *
   * @deprecated since 0.4 — start from a preset and adjust roles with the {@code with*} methods, or
   *     use the canonical constructor with lists per role.
   */
  @Deprecated(since = "0.4")
  public static FrameworkAnnotations of(
      String service,
      String component,
      String controller,
      String restController,
      String transactional,
      String eventListener,
      String applicationModule) {
    return new FrameworkAnnotations(
        "custom",
        List.of(service, component),
        List.of(controller),
        List.of(restController),
        List.of(transactional),
        List.of(eventListener),
        applicationModule == null || applicationModule.isBlank()
            ? List.of()
            : List.of(applicationModule),
        List.of("org.springframework.modulith.NamedInterface"),
        List.of("jakarta.persistence.Entity", "jakarta.persistence.Table"));
  }

  /**
   * The first injectable stereotype, or {@code null} when none is configured.
   *
   * @deprecated since 0.4 — use {@link #injectable()}, which holds every stereotype of the role.
   */
  @Deprecated(since = "0.4")
  public String service() {
    return injectable.isEmpty() ? null : injectable.get(0);
  }

  /**
   * The second injectable stereotype (the first when there is only one), or {@code null}.
   *
   * @deprecated since 0.4 — use {@link #injectable()}, which holds every stereotype of the role.
   */
  @Deprecated(since = "0.4")
  public String component() {
    return injectable.size() > 1 ? injectable.get(1) : service();
  }

  /**
   * The first web-controller stereotype, or {@code null}.
   *
   * @deprecated since 0.4 — use {@link #webController()}.
   */
  @Deprecated(since = "0.4")
  public String controller() {
    return webController.isEmpty() ? null : webController.get(0);
  }

  /**
   * The first module-declaration annotation, or {@code null}.
   *
   * @deprecated since 0.4 — use {@link #moduleDeclaration()}.
   */
  @Deprecated(since = "0.4")
  public String applicationModule() {
    return moduleDeclaration.isEmpty() ? null : moduleDeclaration.get(0);
  }

  /**
   * @deprecated since 0.4 — use {@link #hasModuleDeclaration()}.
   */
  @Deprecated(since = "0.4")
  public boolean hasApplicationModule() {
    return hasModuleDeclaration();
  }
}
