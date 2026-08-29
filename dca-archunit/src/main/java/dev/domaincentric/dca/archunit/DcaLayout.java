package dev.domaincentric.dca.archunit;

import java.util.List;
import java.util.Objects;

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
 *     .withOutgoingSubpackage("out");
 * }</pre>
 *
 * <p>All package patterns returned by this class use ArchUnit's package-matching syntax ({@code ..}
 * for any number of sub-packages, {@code *} for exactly one segment).
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
  private final String useCaseSuffix;
  private final String restControllerSuffix;
  private final List<String> thirdPartyPackagesAllowedInDomain;
  private final FrameworkAnnotations frameworkAnnotations;

  private DcaLayout(
      String basePackage,
      String sharedKernelSubpackage,
      String domainSubpackage,
      String applicationSubpackage,
      String adapterSubpackage,
      String incomingSubpackage,
      String outgoingSubpackage,
      String infrastructureSubpackage,
      String useCaseSuffix,
      String restControllerSuffix,
      List<String> thirdPartyPackagesAllowedInDomain,
      FrameworkAnnotations frameworkAnnotations) {
    this.basePackage = requireSegment(basePackage, "basePackage");
    this.sharedKernelSubpackage = requireSegment(sharedKernelSubpackage, "sharedKernelSubpackage");
    this.domainSubpackage = requireSegment(domainSubpackage, "domainSubpackage");
    this.applicationSubpackage = requireSegment(applicationSubpackage, "applicationSubpackage");
    this.adapterSubpackage = requireSegment(adapterSubpackage, "adapterSubpackage");
    this.incomingSubpackage = requireSegment(incomingSubpackage, "incomingSubpackage");
    this.outgoingSubpackage = requireSegment(outgoingSubpackage, "outgoingSubpackage");
    this.infrastructureSubpackage =
        requireSegment(infrastructureSubpackage, "infrastructureSubpackage");
    this.useCaseSuffix = requireSegment(useCaseSuffix, "useCaseSuffix");
    this.restControllerSuffix = requireSegment(restControllerSuffix, "restControllerSuffix");
    this.thirdPartyPackagesAllowedInDomain = List.copyOf(thirdPartyPackagesAllowedInDomain);
    this.frameworkAnnotations = Objects.requireNonNull(frameworkAnnotations);
  }

  /** The DCA default layout for the given base package. */
  public static DcaLayout forBasePackage(String basePackage) {
    return new DcaLayout(
        basePackage,
        "sharedkernel",
        "domain",
        "application",
        "adapter",
        "incoming",
        "outgoing",
        "infrastructure",
        "UseCase",
        "Resource",
        DEFAULT_THIRD_PARTY_ALLOWED_IN_DOMAIN,
        FrameworkAnnotations.spring());
  }

  private static String requireSegment(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }

  // ---------------------------------------------------------------------------------------------
  // Fluent overrides
  // ---------------------------------------------------------------------------------------------

  public DcaLayout withSharedKernelSubpackage(String value) {
    return copy(
        value,
        domainSubpackage,
        applicationSubpackage,
        adapterSubpackage,
        incomingSubpackage,
        outgoingSubpackage,
        infrastructureSubpackage,
        useCaseSuffix,
        restControllerSuffix,
        thirdPartyPackagesAllowedInDomain,
        frameworkAnnotations);
  }

  public DcaLayout withDomainSubpackage(String value) {
    return copy(
        sharedKernelSubpackage,
        value,
        applicationSubpackage,
        adapterSubpackage,
        incomingSubpackage,
        outgoingSubpackage,
        infrastructureSubpackage,
        useCaseSuffix,
        restControllerSuffix,
        thirdPartyPackagesAllowedInDomain,
        frameworkAnnotations);
  }

  public DcaLayout withApplicationSubpackage(String value) {
    return copy(
        sharedKernelSubpackage,
        domainSubpackage,
        value,
        adapterSubpackage,
        incomingSubpackage,
        outgoingSubpackage,
        infrastructureSubpackage,
        useCaseSuffix,
        restControllerSuffix,
        thirdPartyPackagesAllowedInDomain,
        frameworkAnnotations);
  }

  public DcaLayout withAdapterSubpackage(String value) {
    return copy(
        sharedKernelSubpackage,
        domainSubpackage,
        applicationSubpackage,
        value,
        incomingSubpackage,
        outgoingSubpackage,
        infrastructureSubpackage,
        useCaseSuffix,
        restControllerSuffix,
        thirdPartyPackagesAllowedInDomain,
        frameworkAnnotations);
  }

  /** Name of the incoming (driving/primary) adapter sub-package — {@code "in"} in some projects. */
  public DcaLayout withIncomingSubpackage(String value) {
    return copy(
        sharedKernelSubpackage,
        domainSubpackage,
        applicationSubpackage,
        adapterSubpackage,
        value,
        outgoingSubpackage,
        infrastructureSubpackage,
        useCaseSuffix,
        restControllerSuffix,
        thirdPartyPackagesAllowedInDomain,
        frameworkAnnotations);
  }

  /**
   * Name of the outgoing (driven/secondary) adapter sub-package — {@code "out"} in some projects.
   */
  public DcaLayout withOutgoingSubpackage(String value) {
    return copy(
        sharedKernelSubpackage,
        domainSubpackage,
        applicationSubpackage,
        adapterSubpackage,
        incomingSubpackage,
        value,
        infrastructureSubpackage,
        useCaseSuffix,
        restControllerSuffix,
        thirdPartyPackagesAllowedInDomain,
        frameworkAnnotations);
  }

  public DcaLayout withInfrastructureSubpackage(String value) {
    return copy(
        sharedKernelSubpackage,
        domainSubpackage,
        applicationSubpackage,
        adapterSubpackage,
        incomingSubpackage,
        outgoingSubpackage,
        value,
        useCaseSuffix,
        restControllerSuffix,
        thirdPartyPackagesAllowedInDomain,
        frameworkAnnotations);
  }

  /** Suffix of use-case implementations, e.g. {@code "UseCase"} or {@code "ApplicationService"}. */
  public DcaLayout withUseCaseSuffix(String value) {
    return copy(
        sharedKernelSubpackage,
        domainSubpackage,
        applicationSubpackage,
        adapterSubpackage,
        incomingSubpackage,
        outgoingSubpackage,
        infrastructureSubpackage,
        value,
        restControllerSuffix,
        thirdPartyPackagesAllowedInDomain,
        frameworkAnnotations);
  }

  /** Suffix of REST controllers, e.g. {@code "Resource"} or {@code "Controller"}. */
  public DcaLayout withRestControllerSuffix(String value) {
    return copy(
        sharedKernelSubpackage,
        domainSubpackage,
        applicationSubpackage,
        adapterSubpackage,
        incomingSubpackage,
        outgoingSubpackage,
        infrastructureSubpackage,
        useCaseSuffix,
        value,
        thirdPartyPackagesAllowedInDomain,
        frameworkAnnotations);
  }

  /**
   * Third-party packages the domain layer may depend on (ArchUnit patterns). Replaces the default
   * list ({@code java..}, {@code lombok..}, commons-lang3, commons-collections4, jspecify).
   */
  public DcaLayout withThirdPartyPackagesAllowedInDomain(List<String> patterns) {
    return copy(
        sharedKernelSubpackage,
        domainSubpackage,
        applicationSubpackage,
        adapterSubpackage,
        incomingSubpackage,
        outgoingSubpackage,
        infrastructureSubpackage,
        useCaseSuffix,
        restControllerSuffix,
        patterns,
        frameworkAnnotations);
  }

  /** Adds third-party packages to the domain allow-list. */
  public DcaLayout allowingInDomain(String... patterns) {
    var merged = new java.util.ArrayList<>(thirdPartyPackagesAllowedInDomain);
    merged.addAll(List.of(patterns));
    return withThirdPartyPackagesAllowedInDomain(merged);
  }

  /** Fully qualified names of the framework annotations the rules look for. Defaults to Spring. */
  public DcaLayout withFrameworkAnnotations(FrameworkAnnotations value) {
    return copy(
        sharedKernelSubpackage,
        domainSubpackage,
        applicationSubpackage,
        adapterSubpackage,
        incomingSubpackage,
        outgoingSubpackage,
        infrastructureSubpackage,
        useCaseSuffix,
        restControllerSuffix,
        thirdPartyPackagesAllowedInDomain,
        value);
  }

  private DcaLayout copy(
      String sharedKernelSubpackage,
      String domainSubpackage,
      String applicationSubpackage,
      String adapterSubpackage,
      String incomingSubpackage,
      String outgoingSubpackage,
      String infrastructureSubpackage,
      String useCaseSuffix,
      String restControllerSuffix,
      List<String> thirdPartyPackagesAllowedInDomain,
      FrameworkAnnotations frameworkAnnotations) {
    return new DcaLayout(
        basePackage,
        sharedKernelSubpackage,
        domainSubpackage,
        applicationSubpackage,
        adapterSubpackage,
        incomingSubpackage,
        outgoingSubpackage,
        infrastructureSubpackage,
        useCaseSuffix,
        restControllerSuffix,
        thirdPartyPackagesAllowedInDomain,
        frameworkAnnotations);
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

  public String useCaseSuffix() {
    return useCaseSuffix;
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

  /** {@code base.*.domain..} — the domain layer of every direct sub-package (context). */
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
