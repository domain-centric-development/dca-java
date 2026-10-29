package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.DcaArchitecture.Placement;
import dev.domaincentric.dca.archunit.DcaLayout.Layer;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * A project that declares some of its layers with its own annotations — on a package, inherited by
 * the packages below it, or on a single class — beside the path the library defaults to.
 */
class LayerMarkerTest {

  private static final String APP = Fixtures.ROOT + ".layerannotations.app";
  private static final String BILLING = APP + ".billing";
  private static final String VOCABULARY = Fixtures.ROOT + ".layerannotations.vocabulary.";

  private static DcaLayout pathAndAnnotations() {
    return DcaLayout.forBasePackage(APP)
        .withOutgoingAdapter(
            DcaMarker.anyOf(
                DcaMarker.ofPackage("adapter.outgoing"),
                DcaMarker.ofAnnotation(VOCABULARY + "OutgoingLayer")))
        .withIncomingAdapter(
            DcaMarker.anyOf(
                DcaMarker.ofPackage("adapter.incoming"),
                DcaMarker.ofAnnotation(VOCABULARY + "IncomingLayer")))
        .withDomain(
            DcaMarker.anyOf(
                DcaMarker.ofPackage("domain"), DcaMarker.ofAnnotation(VOCABULARY + "DomainLayer")));
  }

  private static DcaArchitecture arch(DcaLayout layout) {
    return DcaArchitecture.of(layout, new ClassFileImporter().importPackages(APP));
  }

  private static Optional<Placement> placementOf(DcaArchitecture arch, String className) {
    JavaClass javaClass = arch.classes().get(className);
    return arch.placement(javaClass);
  }

  private static DcaRule rule(DcaLayout layout, String id) {
    return DcaRules.all(layout).stream()
        .filter(r -> r.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no such rule: " + id));
  }

  @Test
  void withoutTheAnnotationsTheDeclaredPackageIsInNoLayerAndBothRulesNameIt() {
    DcaLayout layout = DcaLayout.forBasePackage(APP);
    DcaArchitecture arch = arch(layout);

    assertEquals(
        Optional.empty(), placementOf(arch, BILLING + ".out.persistence.JdbcInvoiceStore"));
    AssertionError error =
        assertThrows(AssertionError.class, () -> rule(layout, "DCA-HEX-019").check(arch));
    assertTrue(
        error
            .getMessage()
            .contains(
                BILLING
                    + ".out.persistence.JdbcInvoiceStore implements an output port but sits in no"
                    + " layer"),
        error.getMessage());
    List<String> unplaced = rule(layout, "DCA-LAY-007").observe(arch);
    assertTrue(
        unplaced.contains(BILLING + ".out.persistence.JdbcInvoiceStore (module " + BILLING + ")"),
        unplaced.toString());
  }

  @Test
  void anOutputPortImplementationPlacedInAnotherLayerIsReported() {
    DcaLayout layout =
        DcaLayout.forBasePackage(APP)
            .withIncomingAdapter(
                DcaMarker.anyOf(
                    DcaMarker.ofPackage("adapter.incoming"),
                    DcaMarker.ofAnnotation(VOCABULARY + "OutgoingLayer")));
    AssertionError error =
        assertThrows(AssertionError.class, () -> rule(layout, "DCA-HEX-019").check(arch(layout)));

    assertTrue(
        error
            .getMessage()
            .contains(
                BILLING
                    + ".lone.MailGateway implements an output port but is placed in the"
                    + " incomingAdapter layer (class "
                    + BILLING
                    + ".lone)"),
        error.getMessage());
  }

  @Test
  void aPackageDeclarationHoldsForItsSubtree() {
    Placement placement =
        placementOf(arch(pathAndAnnotations()), BILLING + ".out.persistence.JdbcInvoiceStore")
            .orElseThrow();

    assertEquals(Layer.OUTGOING_ADAPTER, placement.layer());
    assertEquals(BILLING + ".out", placement.layerPackage());
    assertEquals(Placement.Source.PACKAGE, placement.source());
  }

  @Test
  void aClassAnnotationPlacesTheClassWhereverItSits() {
    Placement placement =
        placementOf(arch(pathAndAnnotations()), BILLING + ".lone.MailGateway").orElseThrow();

    assertEquals(Layer.OUTGOING_ADAPTER, placement.layer());
    assertEquals(Placement.Source.CLASS, placement.source());
  }

  @Test
  void theAnnotationWinsOverThePath() {
    DcaArchitecture arch = arch(pathAndAnnotations());

    assertEquals(
        Layer.INCOMING_ADAPTER,
        placementOf(arch, BILLING + ".adapter.outgoing.legacy.LegacyHook").orElseThrow().layer());
    assertEquals(
        Placement.Source.PATH,
        placementOf(arch, BILLING + ".adapter.incoming.web.InvoicePage").orElseThrow().source());
    assertEquals(
        Layer.DOMAIN, placementOf(arch, BILLING + ".domain.model.Invoice").orElseThrow().layer());
  }

  @Test
  void theOutputPortImplementationsAreInTheOutgoingAdaptersOnceDeclared() {
    DcaLayout layout = pathAndAnnotations();

    rule(layout, "DCA-HEX-019").check(arch(layout));
  }

  @Test
  void anOverrideAndAContradictingSubpackageAreNoticed() {
    List<String> notices = arch(pathAndAnnotations()).layerNotices();

    assertTrue(
        notices.contains(
            "package "
                + BILLING
                + ".adapter.outgoing.legacy: path says outgoingAdapter, annotation says"
                + " incomingAdapter, incomingAdapter holds"),
        notices.toString());
    assertTrue(
        notices.contains(
            "package "
                + BILLING
                + ".conflict.inner declares domain inside "
                + BILLING
                + ".conflict, which declares outgoingAdapter - one layer per subtree"),
        notices.toString());
  }

  @Test
  void aLayerGivenOnlyByAnnotationHasNoPath() {
    DcaLayout layout =
        DcaLayout.forBasePackage(APP)
            .withIncomingAdapter(DcaMarker.ofAnnotation(VOCABULARY + "IncomingLayer"));
    DcaArchitecture arch = arch(layout);

    assertFalse(layout.layerHasPath(Layer.INCOMING_ADAPTER));
    assertEquals(
        Optional.empty(), placementOf(arch, BILLING + ".adapter.incoming.web.InvoicePage"));
    assertEquals(
        Layer.INCOMING_ADAPTER,
        placementOf(arch, BILLING + ".adapter.outgoing.legacy.LegacyHook").orElseThrow().layer());
  }

  @Test
  void theReportNamesTheLayersRecognisedOtherwise() {
    assertEquals("path (library default)", DcaLayout.forBasePackage(APP).layersReport());
    assertEquals(
        "outgoingAdapter=anyOf(ofPackage(adapter.outgoing), ofAnnotation("
            + VOCABULARY
            + "OutgoingLayer))",
        DcaLayout.forBasePackage(APP)
            .withOutgoingAdapter(
                DcaMarker.anyOf(
                    DcaMarker.ofPackage("adapter.outgoing"),
                    DcaMarker.ofAnnotation(VOCABULARY + "OutgoingLayer")))
            .layersReport());
  }

  @Test
  void aLayerTakesNoTypeAndAnAdapterPackageStaysBelowTheAdapterSegment() {
    DcaLayout layout = DcaLayout.forBasePackage(APP);

    IllegalArgumentException type =
        assertThrows(
            IllegalArgumentException.class,
            () -> layout.withDomain(DcaMarker.ofType("com.acme.DomainThing")));
    assertTrue(type.getMessage().contains("ofPackage or ofAnnotation"), type.getMessage());
    IllegalArgumentException adapter =
        assertThrows(
            IllegalArgumentException.class,
            () -> layout.withOutgoingAdapter(DcaMarker.ofPackage("secondary")));
    assertTrue(adapter.getMessage().contains("withAdapterSubpackage"), adapter.getMessage());
    assertEquals(
        "out", layout.withOutgoingAdapter(DcaMarker.ofPackage("adapter.out")).outgoingSubpackage());
  }

  @Test
  void anyOfFlattensAndAOneMemberAnyOfIsThatMember() {
    DcaMarker annotation = DcaMarker.ofAnnotation("com.acme.Layer");

    assertEquals(annotation, DcaMarker.anyOf(annotation));
    assertEquals(
        3,
        DcaMarker.anyOf(
                DcaMarker.anyOf(DcaMarker.ofPackage("domain"), annotation),
                DcaMarker.ofAnnotation("org.example.Layer"))
            .alternatives()
            .size());
  }
}
