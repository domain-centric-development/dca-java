package dev.domaincentric.dca.archunit;

import java.util.Objects;

/**
 * Fully qualified names of framework annotations the DCA rules refer to. The rule library has no
 * compile-time dependency on any framework; annotations are matched by name so the same rules work
 * with Spring, Jakarta CDI, Micronaut or a hand-rolled container.
 *
 * <p>Use {@link #spring()} for the defaults or build your own with {@link #of}.
 *
 * @param service stereotype for application services / use-case beans
 * @param component generic component stereotype
 * @param controller MVC controller stereotype
 * @param restController REST controller stereotype
 * @param transactional transaction demarcation
 * @param eventListener in-process event listener
 * @param applicationModule module declaration on {@code package-info} (Spring Modulith's
 *     {@code @ApplicationModule}); may be {@code null} when the project uses no module system
 */
public record FrameworkAnnotations(
    String service,
    String component,
    String controller,
    String restController,
    String transactional,
    String eventListener,
    String applicationModule) {

  public FrameworkAnnotations {
    Objects.requireNonNull(service, "service");
    Objects.requireNonNull(component, "component");
    Objects.requireNonNull(controller, "controller");
    Objects.requireNonNull(restController, "restController");
    Objects.requireNonNull(transactional, "transactional");
    Objects.requireNonNull(eventListener, "eventListener");
  }

  /** Spring Framework + Spring Modulith annotation names. */
  public static FrameworkAnnotations spring() {
    return new FrameworkAnnotations(
        "org.springframework.stereotype.Service",
        "org.springframework.stereotype.Component",
        "org.springframework.stereotype.Controller",
        "org.springframework.web.bind.annotation.RestController",
        "org.springframework.transaction.annotation.Transactional",
        "org.springframework.context.event.EventListener",
        "org.springframework.modulith.ApplicationModule");
  }

  public static FrameworkAnnotations of(
      String service,
      String component,
      String controller,
      String restController,
      String transactional,
      String eventListener,
      String applicationModule) {
    return new FrameworkAnnotations(
        service,
        component,
        controller,
        restController,
        transactional,
        eventListener,
        applicationModule);
  }

  /** Whether a module-declaration annotation is configured. */
  public boolean hasApplicationModule() {
    return applicationModule != null && !applicationModule.isBlank();
  }
}
