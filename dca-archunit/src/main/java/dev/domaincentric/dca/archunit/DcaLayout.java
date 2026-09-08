package dev.domaincentric.dca.archunit;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Describes how a Domain-Centric Architecture code base is laid out in packages, so that the DCA
 * rules can be applied to any project regardless of its base package or naming conventions.
 *
 * <p>Obtain the DCA defaults with {@link #forBasePackage(String)} and override individual settings
 * through the fluent {@code with*} methods:
 *
 * <pre>{@code
 * DcaLayout layout = DcaLayout.forBasePackage("com.acme.shop")
 *     .withIncomingSubpackage("in")
 *     .withOutgoingSubpackage("out")
 *     .withApiSubpackage("contract");
 * }</pre>
 *
 * <p>All package patterns returned by this class use ArchUnit's package-matching syntax ({@code ..}
 * for any number of sub-packages, {@code *} for exactly one segment).
 *
 * <p><b>Wildcard patterns versus discovered contexts.</b> The no-argument accessors ({@link
 * #domainPattern()} and its siblings) build {@code base.*.domain..}, where {@code *} is exactly one
 * segment — they therefore only ever match a bounded context that is a direct child of the base
 * package. They are kept for projects that have not declared their contexts yet, and for tooling
 * that needs a pattern without an imported class graph. <b>The rules do not use them.</b> Every
 * rule selects through {@code DcaArchitecture}'s discovery accessors ({@code
 * contextDomainPatterns()}, {@code allDomainPatternsWithSharedKernel()}, …), which are built from
 * the packages that actually carry {@code @BoundedContext} and so work at any depth: {@code
 * base.sales.order} and a single-context application that annotates its base package are both
 * governed.
 *
 * <p>The wildcard is deliberately not loosened to {@code base..domain..}: that would also match any
 * package merely *named* {@code domain} further down, such as an outgoing adapter mapping to a
 * foreign model. A module is instead found structurally by {@code DcaArchitecture.moduleRoots()} —
 * the shortest prefix that owns a layer — which needs no annotation and is not fooled by such a
 * package.
 */
public final class DcaLayout {

  /** Package of the dca-building-blocks markers, as an ArchUnit pattern. */
  public static final String BUILDING_BLOCKS_PACKAGE = "dev.domaincentric.dca.buildingblocks..";

  public static final String BUILDING_BLOCKS_TACTICAL_PACKAGE =
      "dev.domaincentric.dca.buildingblocks.ddd.tactical..";
  public static final String BUILDING_BLOCKS_STRATEGIC_PACKAGE =
      "dev.domaincentric.dca.buildingblocks.ddd.strategic..";
  public static final String BUILDING_BLOCKS_PORT_PACKAGE =
      "dev.domaincentric.dca.buildingblocks.hexagonal.port..";
  public static final String BUILDING_BLOCKS_PORT_IN_PACKAGE =
      "dev.domaincentric.dca.buildingblocks.hexagonal.port.in..";
  public static final String BUILDING_BLOCKS_PORT_OUT_PACKAGE =
      "dev.domaincentric.dca.buildingblocks.hexagonal.port.out..";

  private static final List<String> DEFAULT_THIRD_PARTY_ALLOWED_IN_DOMAIN =
      List.of(
          "java..",
          "lombok..",
          "org.apache.commons.lang3..",
          "org.apache.commons.collections4..",
          "org.jspecify.annotations..");

  private final String basePackage;
  private final String sharedKernelSubpackage;
  private final String domainSubpackage;
  private final String applicationSubpackage;
  private final String adapterSubpackage;
  private final String incomingSubpackage;
  private final String outgoingSubpackage;
  private final String infrastructureSubpackage;
  private final String apiSubpackage;
  private final String eventsSubpackage;
  private final String useCaseSuffix;
  private final String controllerSuffix;
  private final String restControllerSuffix;
  private final List<String> thirdPartyPackagesAllowedInDomain;
  private final FrameworkAnnotations frameworkAnnotations;

  private DcaLayout(Settings settings) {
    this.basePackage = requirePackage(settings.basePackage, "basePackage");
    this.sharedKernelSubpackage =
        requireSegment(settings.sharedKernelSubpackage, "sharedKernelSubpackage");
    this.domainSubpackage = requireSegment(settings.domainSubpackage, "domainSubpackage");
    this.applicationSubpackage =
        requireSegment(settings.applicationSubpackage, "applicationSubpackage");
    this.adapterSubpackage = requireSegment(settings.adapterSubpackage, "adapterSubpackage");
    this.incomingSubpackage = requireSegment(settings.incomingSubpackage, "incomingSubpackage");
    this.outgoingSubpackage = requireSegment(settings.outgoingSubpackage, "outgoingSubpackage");
    this.infrastructureSubpackage =
        requireSegment(settings.infrastructureSubpackage, "infrastructureSubpackage");
    this.apiSubpackage = requireSegment(settings.apiSubpackage, "apiSubpackage");
    this.eventsSubpackage = requireSegment(settings.eventsSubpackage, "eventsSubpackage");
    if (apiSubpackage.equals(eventsSubpackage)) {
      throw new IllegalArgumentException(
          "apiSubpackage and eventsSubpackage must differ, both are '" + apiSubpackage + "'");
    }
    this.useCaseSuffix = requireSuffix(settings.useCaseSuffix, "useCaseSuffix");
    this.controllerSuffix = requireSuffix(settings.controllerSuffix, "controllerSuffix");
    this.restControllerSuffix =
        requireSuffix(settings.restControllerSuffix, "restControllerSuffix");
    this.thirdPartyPackagesAllowedInDomain =
        List.copyOf(
            Objects.requireNonNull(
                settings.thirdPartyPackagesAllowedInDomain, "thirdPartyPackagesAllowedInDomain"));
    this.frameworkAnnotations =
        Objects.requireNonNull(settings.frameworkAnnotations, "frameworkAnnotations");
  }

  /** The DCA default layout for the given base package. */
  public static DcaLayout forBasePackage(String basePackage) {
    Settings defaults = new Settings();
    defaults.basePackage = basePackage;
    defaults.sharedKernelSubpackage = "sharedkernel";
    defaults.domainSubpackage = "domain";
    defaults.applicationSubpackage = "application";
    defaults.adapterSubpackage = "adapter";
    defaults.incomingSubpackage = "incoming";
    defaults.outgoingSubpackage = "outgoing";
    defaults.infrastructureSubpackage = "infrastructure";
    defaults.apiSubpackage = "api";
    defaults.eventsSubpackage = "events";
    defaults.useCaseSuffix = "UseCase";
    defaults.controllerSuffix = "Controller";
    defaults.restControllerSuffix = "Resource";
    defaults.thirdPartyPackagesAllowedInDomain = DEFAULT_THIRD_PARTY_ALLOWED_IN_DOMAIN;
    defaults.frameworkAnnotations = FrameworkAnnotations.spring();
    return new DcaLayout(defaults);
  }

  /** A complete package name: dot-separated Java identifiers. */
  private static String requirePackage(String value, String name) {
    requireNotBlank(value, name);
    for (String segment : value.split("\\.", -1)) {
      if (!isJavaIdentifier(segment)) {
        throw new IllegalArgumentException(name + " must be a package name, was '" + value + "'");
      }
    }
    return value;
  }

  /** One package segment: a single Java identifier, no dots. */
  private static String requireSegment(String value, String name) {
    requireNotBlank(value, name);
    if (!isJavaIdentifier(value)) {
      throw new IllegalArgumentException(
          name + " must be a single package segment, was '" + value + "'");
    }
    return value;
  }

  /** A class-name suffix: part of a Java identifier. */
  private static String requireSuffix(String value, String name) {
    requireNotBlank(value, name);
    for (int i = 0; i < value.length(); i++) {
      if (!Character.isJavaIdentifierPart(value.charAt(i))) {
        throw new IllegalArgumentException(
            name + " must be part of a class name, was '" + value + "'");
      }
    }
    return value;
  }

  private static void requireNotBlank(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
  }

  private static boolean isJavaIdentifier(String segment) {
    if (segment.isEmpty() || !Character.isJavaIdentifierStart(segment.charAt(0))) {
      return false;
    }
    for (int i = 1; i < segment.length(); i++) {
      if (!Character.isJavaIdentifierPart(segment.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  // ---------------------------------------------------------------------------------------------
  // Fluent overrides
  // ---------------------------------------------------------------------------------------------

  public DcaLayout withSharedKernelSubpackage(String value) {
    return copy(settings -> settings.sharedKernelSubpackage = value);
  }

  public DcaLayout withDomainSubpackage(String value) {
    return copy(settings -> settings.domainSubpackage = value);
  }

  public DcaLayout withApplicationSubpackage(String value) {
    return copy(settings -> settings.applicationSubpackage = value);
  }

  public DcaLayout withAdapterSubpackage(String value) {
    return copy(settings -> settings.adapterSubpackage = value);
  }

  /** Name of the incoming (driving/primary) adapter sub-package — {@code "in"} in some projects. */
  public DcaLayout withIncomingSubpackage(String value) {
    return copy(settings -> settings.incomingSubpackage = value);
  }

  /**
   * Name of the outgoing (driven/secondary) adapter sub-package — {@code "out"} in some projects.
   */
  public DcaLayout withOutgoingSubpackage(String value) {
    return copy(settings -> settings.outgoingSubpackage = value);
  }

  public DcaLayout withInfrastructureSubpackage(String value) {
    return copy(settings -> settings.infrastructureSubpackage = value);
  }

  /**
   * Sub-package of a module's <em>synchronous</em> published contract, e.g. {@code "api"} (default)
   * or {@code "contract"}. Together with {@link #withEventsSubpackage(String)} it is the only part
   * of a module another module's adapters may depend on; the context-map rules and renderer use the
   * same name for the channel.
   */
  public DcaLayout withApiSubpackage(String value) {
    return copy(settings -> settings.apiSubpackage = value);
  }

  /**
   * Sub-package of a module's <em>asynchronous</em> published contract — its integration events —
   * e.g. {@code "events"} (default) or {@code "published"}.
   */
  public DcaLayout withEventsSubpackage(String value) {
    return copy(settings -> settings.eventsSubpackage = value);
  }

  /** Suffix of use-case implementations, e.g. {@code "UseCase"} or {@code "ApplicationService"}. */
  public DcaLayout withUseCaseSuffix(String value) {
    return copy(settings -> settings.useCaseSuffix = value);
  }

  /**
   * Suffix of MVC (server-rendered) controllers, e.g. {@code "Controller"} (default) or {@code
   * "Page"}. Read by the naming rule for classes carrying the configured {@code @Controller}
   * annotation and by the rule that keeps controllers away from repositories; the REST suffix is
   * configured separately.
   */
  public DcaLayout withControllerSuffix(String value) {
    return copy(settings -> settings.controllerSuffix = value);
  }

  /** Suffix of REST controllers, e.g. {@code "Resource"} or {@code "Controller"}. */
  public DcaLayout withRestControllerSuffix(String value) {
    return copy(settings -> settings.restControllerSuffix = value);
  }

  /**
   * Third-party packages the domain layer may depend on (ArchUnit patterns). Replaces the default
   * list ({@code java..}, {@code lombok..}, commons-lang3, commons-collections4, jspecify).
   */
  public DcaLayout withThirdPartyPackagesAllowedInDomain(List<String> patterns) {
    return copy(settings -> settings.thirdPartyPackagesAllowedInDomain = patterns);
  }

  /** Adds third-party packages to the domain allow-list. */
  public DcaLayout allowingInDomain(String... patterns) {
    var merged = new java.util.ArrayList<>(thirdPartyPackagesAllowedInDomain);
    merged.addAll(List.of(patterns));
    return withThirdPartyPackagesAllowedInDomain(merged);
  }

  /** Fully qualified names of the framework annotations the rules look for. Defaults to Spring. */
  public DcaLayout withFrameworkAnnotations(FrameworkAnnotations value) {
    return copy(settings -> settings.frameworkAnnotations = value);
  }

  /** This layout with one setting changed; validation runs in the constructor as always. */
  private DcaLayout copy(Consumer<Settings> change) {
    Settings settings = new Settings();
    settings.basePackage = basePackage;
    settings.sharedKernelSubpackage = sharedKernelSubpackage;
    settings.domainSubpackage = domainSubpackage;
    settings.applicationSubpackage = applicationSubpackage;
    settings.adapterSubpackage = adapterSubpackage;
    settings.incomingSubpackage = incomingSubpackage;
    settings.outgoingSubpackage = outgoingSubpackage;
    settings.infrastructureSubpackage = infrastructureSubpackage;
    settings.apiSubpackage = apiSubpackage;
    settings.eventsSubpackage = eventsSubpackage;
    settings.useCaseSuffix = useCaseSuffix;
    settings.controllerSuffix = controllerSuffix;
    settings.restControllerSuffix = restControllerSuffix;
    settings.thirdPartyPackagesAllowedInDomain = thirdPartyPackagesAllowedInDomain;
    settings.frameworkAnnotations = frameworkAnnotations;
    change.accept(settings);
    return new DcaLayout(settings);
  }

  /** The mutable carrier the fluent overrides edit before the constructor validates them. */
  private static final class Settings {
    String basePackage;
    String sharedKernelSubpackage;
    String domainSubpackage;
    String applicationSubpackage;
    String adapterSubpackage;
    String incomingSubpackage;
    String outgoingSubpackage;
    String infrastructureSubpackage;
    String apiSubpackage;
    String eventsSubpackage;
    String useCaseSuffix;
    String controllerSuffix;
    String restControllerSuffix;
    List<String> thirdPartyPackagesAllowedInDomain;
    FrameworkAnnotations frameworkAnnotations;
  }

  // ---------------------------------------------------------------------------------------------
  // Plain values
  // ---------------------------------------------------------------------------------------------

  public String basePackage() {
    return basePackage;
  }

  public String sharedKernelSubpackage() {
    return sharedKernelSubpackage;
  }

  public String domainSubpackage() {
    return domainSubpackage;
  }

  public String applicationSubpackage() {
    return applicationSubpackage;
  }

  public String adapterSubpackage() {
    return adapterSubpackage;
  }

  public String incomingSubpackage() {
    return incomingSubpackage;
  }

  public String outgoingSubpackage() {
    return outgoingSubpackage;
  }

  public String infrastructureSubpackage() {
    return infrastructureSubpackage;
  }

  public String apiSubpackage() {
    return apiSubpackage;
  }

  public String eventsSubpackage() {
    return eventsSubpackage;
  }

  /**
   * The published sub-packages of a module, {@code api} then {@code events}: DCA's in-process
   * contract convention — package names, not framework annotations — and the only part of a module
   * another module's adapters may depend on.
   */
  public List<String> publishedSubpackages() {
    return List.of(apiSubpackage, eventsSubpackage);
  }

  public String useCaseSuffix() {
    return useCaseSuffix;
  }

  public String controllerSuffix() {
    return controllerSuffix;
  }

  public String restControllerSuffix() {
    return restControllerSuffix;
  }

  public List<String> thirdPartyPackagesAllowedInDomain() {
    return thirdPartyPackagesAllowedInDomain;
  }

  public FrameworkAnnotations frameworkAnnotations() {
    return frameworkAnnotations;
  }

  // ---------------------------------------------------------------------------------------------
  // Derived package names and patterns
  // ---------------------------------------------------------------------------------------------

  /** {@code base.sharedkernel} (no pattern suffix). */
  public String sharedKernelPackage() {
    return basePackage + "." + sharedKernelSubpackage;
  }

  /** {@code base.sharedkernel..} */
  public String sharedKernelPattern() {
    return sharedKernelPackage() + "..";
  }

  /** {@code base.sharedkernel.domain..} */
  public String sharedKernelDomainPattern() {
    return sharedKernelPackage() + "." + domainSubpackage + "..";
  }

  /** {@code base.sharedkernel.domain.model..} */
  public String sharedKernelDomainModelPattern() {
    return sharedKernelPackage() + "." + domainSubpackage + ".model..";
  }

  /** {@code base.infrastructure} (no pattern suffix). */
  public String infrastructurePackage() {
    return basePackage + "." + infrastructureSubpackage;
  }

  /** {@code base.infrastructure..} */
  public String infrastructurePattern() {
    return infrastructurePackage() + "..";
  }

  /** {@code base.cart.infrastructure} — a module's own infrastructure package (no suffix). */
  public String infrastructurePackage(String modulePackage) {
    return modulePackage + "." + infrastructureSubpackage;
  }

  /**
   * The sub-package a consumed channel maps to: {@link #apiSubpackage()} for {@link
   * Upstream.Consumes#API}, {@link #eventsSubpackage()} for {@link Upstream.Consumes#EVENTS}. The
   * context-map rules and the renderer name channels through this one method.
   */
  public String channelSubpackage(Upstream.Consumes channel) {
    return channel == Upstream.Consumes.API ? apiSubpackage : eventsSubpackage;
  }

  /**
   * {@code base.*.domain..} — the domain layer of every <em>direct</em> sub-package. Matches a
   * bounded context only when it is a direct child of the base package; prefer {@code
   * DcaArchitecture.contextDomainPatterns()}, which is derived from the declared contexts and holds
   * at any depth.
   */
  public String domainPattern() {
    return basePackage + ".*." + domainSubpackage + "..";
  }

  /** {@code base.*.domain.model..} */
  public String domainModelPattern() {
    return basePackage + ".*." + domainSubpackage + ".model..";
  }

  /** {@code base.*.application..} */
  public String applicationPattern() {
    return basePackage + ".*." + applicationSubpackage + "..";
  }

  /** {@code base.*.application.shared..} — output ports shared by the use cases of one context. */
  public String sharedOutputPortPattern() {
    return basePackage + ".*." + applicationSubpackage + ".shared..";
  }

  /** {@code base.*.adapter..} */
  public String adapterPattern() {
    return basePackage + ".*." + adapterSubpackage + "..";
  }

  /** {@code base.*.adapter.incoming..} */
  public String incomingAdapterPattern() {
    return basePackage + ".*." + adapterSubpackage + "." + incomingSubpackage + "..";
  }

  /** {@code base.*.adapter.outgoing..} */
  public String outgoingAdapterPattern() {
    return basePackage + ".*." + adapterSubpackage + "." + outgoingSubpackage + "..";
  }

  // Per-context variants — {@code contextPackage} without trailing dots.

  public String domainPattern(String contextPackage) {
    return contextPackage + "." + domainSubpackage + "..";
  }

  public String domainModelPattern(String contextPackage) {
    return contextPackage + "." + domainSubpackage + ".model..";
  }

  public String applicationPattern(String contextPackage) {
    return contextPackage + "." + applicationSubpackage + "..";
  }

  public String sharedOutputPortPattern(String contextPackage) {
    return contextPackage + "." + applicationSubpackage + ".shared..";
  }

  public String adapterPattern(String contextPackage) {
    return contextPackage + "." + adapterSubpackage + "..";
  }

  public String incomingAdapterPattern(String contextPackage) {
    return contextPackage + "." + adapterSubpackage + "." + incomingSubpackage + "..";
  }

  public String outgoingAdapterPattern(String contextPackage) {
    return contextPackage + "." + adapterSubpackage + "." + outgoingSubpackage + "..";
  }

  @Override
  public String toString() {
    return "DcaLayout[" + basePackage + "]";
  }
}
