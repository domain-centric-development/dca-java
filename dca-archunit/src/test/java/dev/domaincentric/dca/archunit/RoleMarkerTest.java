package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/**
 * A project that marks its building blocks and its commands with annotations of its own instead of
 * supertypes or names.
 */
class RoleMarkerTest {

  private static final String APP = Fixtures.ROOT + ".roleannotations.app";
  private static final String MODEL = APP + ".orders.domain.model.";
  private static final String VOCABULARY = Fixtures.ROOT + ".roleannotations.vocabulary.";

  private static DcaLayout annotated() {
    return DcaLayout.forBasePackage(APP)
        .withMarkers(
            DcaMarkers.dca()
                .named("annotated")
                .withValue(DcaMarker.ofAnnotation(VOCABULARY + "ValueObject"))
                .withCommand(DcaMarker.ofAnnotation(VOCABULARY + "Command")));
  }

  private static DcaArchitecture arch(DcaLayout layout) {
    return DcaArchitecture.of(layout, new ClassFileImporter().importPackages(APP));
  }

  private static JavaClass type(DcaArchitecture arch, String simpleName) {
    return arch.classes().get(MODEL + simpleName);
  }

  private static DcaRule rule(DcaLayout layout, String id) {
    return DcaRules.all(layout).stream()
        .filter(r -> r.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no such rule: " + id));
  }

  @Test
  void anAnnotationRecognisesABuildingBlock() {
    DcaArchitecture arch = arch(annotated());
    DcaMarkers markers = arch.layout().markers();

    assertTrue(markers.is("value").test(type(arch, "Amount")));
    assertFalse(markers.is("value").test(type(arch, "Order")));
    assertFalse(DcaMarkers.dca().is("value").test(type(arch, "Amount")));
  }

  @Test
  void anAnnotatedOneFieldValueIsAWrapperForTheBarePrimitiveRule() {
    DcaLayout layout = annotated();
    rule(layout, "DCA-TAC-025").check(arch(layout));

    DcaLayout unmarked = DcaLayout.forBasePackage(APP);
    AssertionError error =
        assertThrows(
            AssertionError.class, () -> rule(unmarked, "DCA-TAC-025").check(arch(unmarked)));
    assertTrue(error.getMessage().contains(MODEL + "Amount"), error.getMessage());
  }

  @Test
  void commandsAreRecognisedBySuffixByDefaultAndByTheProjectsAnnotationOnceConfigured() {
    DcaArchitecture byDefault = arch(DcaLayout.forBasePackage(APP));
    DcaArchitecture byAnnotation = arch(annotated());

    assertTrue(
        byDefault.layout().markers().is("command").test(type(byDefault, "ShowOrderCommand")));
    assertFalse(
        byDefault.layout().markers().is("command").test(type(byDefault, "PlaceOrderRequest")));
    assertTrue(
        byAnnotation
            .layout()
            .markers()
            .is("command")
            .test(type(byAnnotation, "PlaceOrderRequest")));
    assertFalse(
        byAnnotation.layout().markers().is("command").test(type(byAnnotation, "ShowOrderCommand")));
  }

  @Test
  void theCommandRuleFollowsTheConfiguredRole() {
    DcaLayout layout = annotated();
    AssertionError error =
        assertThrows(AssertionError.class, () -> rule(layout, "DCA-USE-002").check(arch(layout)));

    assertTrue(error.getMessage().contains("PlaceOrderRequest"), error.getMessage());
    assertFalse(error.getMessage().contains("ShowOrderCommand"), error.getMessage());
  }

  @Test
  void aBuildingBlockTakesNoPackageAndNoSuffix() {
    DcaMarkers markers = DcaMarkers.dca();

    IllegalArgumentException byPackage =
        assertThrows(
            IllegalArgumentException.class,
            () -> markers.withValue(DcaMarker.ofPackage("domain.model.values")));
    assertTrue(byPackage.getMessage().contains("not what it is"), byPackage.getMessage());
    assertThrows(
        IllegalArgumentException.class, () -> markers.withValue(DcaMarker.ofSuffix("Value")));
    assertThrows(
        IllegalArgumentException.class,
        () -> markers.withBoundedContext(DcaMarker.ofType("com.acme.Context")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            markers.withRepository(
                DcaMarker.anyOf(DcaMarker.ofType("com.acme.A"), DcaMarker.ofType("com.acme.B"))));
  }

  @Test
  void aPlainTypeStaysTheRolesTypeAndTheReportNamesTheRest() {
    DcaMarkers markers =
        DcaMarkers.dca()
            .withAggregateRoot(DcaMarker.ofType("com.acme.Root"))
            .withValue(
                DcaMarker.anyOf(
                    DcaMarker.ofType("com.acme.Value"),
                    DcaMarker.ofAnnotation("com.acme.ValueObject")));

    assertEquals("com.acme.Root", markers.aggregateRoot());
    assertEquals("com.acme.Value", markers.value());
    assertEquals(DcaMarker.ofType("com.acme.Root"), markers.marker("aggregateRoot"));
    assertFalse(markers.recognisedOtherwise().containsKey("aggregateRoot"));
    assertEquals(
        "dca (aggregateRoot=com.acme.Root, value=anyOf(ofType(com.acme.Value),"
            + " ofAnnotation(com.acme.ValueObject)))",
        DcaLayout.forBasePackage(APP).withMarkers(markers).markersReport());
    assertTrue(DcaMarkers.dca().isDefault());
    assertFalse(DcaMarkers.dca().withQuery(DcaMarker.ofSuffix("Read")).isDefault());
  }

  @Test
  void theBarePrimitiveRulePassesOverFailuresAndAValuesOwnErrorShapesAndExplainsAOneFieldType() {
    String app = Fixtures.ROOT + ".barevalues.app";
    String model = app + ".orders.domain.model.";
    DcaLayout layout = DcaLayout.forBasePackage(app);
    AssertionError error =
        assertThrows(
            AssertionError.class,
            () ->
                rule(layout, "DCA-TAC-025")
                    .check(
                        DcaArchitecture.of(layout, new ClassFileImporter().importPackages(app))));
    String message = error.getMessage();

    assertFalse(message.contains("OrderRefused"), message);
    assertFalse(message.contains("EmptyError"), message);
    assertFalse(message.contains("TooLongError"), message);
    assertFalse(message.contains(model + "PostalCode:"), message);
    assertTrue(
        message.contains(
            model
                + "Street: field name is String - it has one field: mark it as a value or"
                + " an id (ofType(dev.domaincentric.dca.buildingblocks.ddd.tactical.Value)) to count"
                + " as a wrapper"),
        message);
  }
}
