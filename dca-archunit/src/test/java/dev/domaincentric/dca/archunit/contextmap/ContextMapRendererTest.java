package dev.domaincentric.dca.archunit.contextmap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContextMapRendererTest {

  private static final String PKG = "dev.domaincentric.dca.archunit.fixtures.contextmaprender";
  private static DcaArchitecture arch;

  @BeforeAll
  static void load() {
    arch =
        DcaArchitecture.of(
            DcaLayout.forBasePackage(PKG), new ClassFileImporter().importPackages(PKG));
  }

  private static List<String> lines(String md) {
    return List.of(md.split("\n"));
  }

  @Test
  void rendersBoundedContextTableSortedByModule() {
    List<String> lines = lines(ContextMapRenderer.of(arch).render());
    assertEquals("# Context Map", lines.get(0));
    int cart = lines.indexOf("| cart | Shopping Cart | Carts of guests and customers | — |");
    int catalog =
        lines.indexOf("| catalog | Product Catalog | Master data of sellable products | api |");
    int shipping = lines.indexOf("| shipping | Shipping | Parcel dispatch | — |");
    assertTrue(cart > 0 && catalog > cart && shipping > catalog, String.join("\n", lines));
  }

  @Test
  void rendersUpstreamsExternalSystemsAndPartnerships() {
    String md = ContextMapRenderer.of(arch).render();
    assertTrue(
        md.contains(
            "| cart | catalog | api | ACL | implemented | Cart needs product master data |"));
    assertTrue(
        md.contains(
            "| cart | catalog | events | ACL | implemented | Cart needs product master data |"));
    assertTrue(
        md.contains("| shipping | cart | events | Conformist | planned | Ship what was ordered |"));
    assertTrue(
        md.contains(
            "| shipping | Carrier API | outbound | REST | Shipment labels | ACL | implemented |"
                + " Labels are printed by the carrier |"));
    assertTrue(md.contains("| cart ↔ catalog | Catalog and cart evolve together |"));
  }

  @Test
  void rendersMermaidDiagram() {
    String md = ContextMapRenderer.of(arch).render();
    assertTrue(md.contains("```mermaid\ngraph LR\n"));
    assertTrue(md.contains("  catalog[\"Product Catalog<br/><i>api</i>\"]"));
    assertTrue(md.contains("  cart -->|\"ACL / api\"| catalog"));
    assertTrue(md.contains("  cart -.->|\"ACL / events\"| catalog"));
    assertTrue(md.contains("  shipping -.->|\"Conformist / events / planned\"| cart"));
    assertTrue(md.contains("  ext_carrier_api[[\"Carrier API\"]]"));
    assertTrue(md.contains("  shipping -->|\"ACL / REST\"| ext_carrier_api"));
    assertTrue(md.contains("  cart ---|\"Partnership\"| catalog"));
  }

  @Test
  void optionsControlSections() {
    String md =
        ContextMapRenderer.of(arch)
            .withMermaid(false)
            .includeExternalSystems(false)
            .includePlanned(false)
            .withTitle("Strategic Map")
            .render();
    assertTrue(md.startsWith("# Strategic Map\n"));
    assertFalse(md.contains("```mermaid"));
    assertFalse(md.contains("## External systems"));
    assertFalse(md.contains("Carrier API"));
    assertFalse(md.contains("planned"), md);
    assertTrue(md.contains("## Upstream relationships"));
  }

  /**
   * Node ids are normalised with {@link java.util.Locale#ROOT}: under a Turkish default locale
   * {@code "API".toLowerCase()} would yield a dotless i and the node {@code ext_carrier_ap_}, so
   * the rendered file would depend on the machine — and disagree with the collision check of {@code
   * DCA-MAP-003}, which normalises the same names.
   */
  @Test
  void externalNodeIdsDoNotDependOnTheDefaultLocale() {
    Locale before = Locale.getDefault();
    Locale.setDefault(Locale.forLanguageTag("tr-TR"));
    try {
      String md = ContextMapRenderer.of(arch).render();
      assertTrue(md.contains("  ext_carrier_api[[\"Carrier API\"]]"), md);
      assertEquals("ext_carrier_api", ContextMapRenderer.externalSystemNodeId("Carrier API"));
    } finally {
      Locale.setDefault(before);
    }
  }

  /** Annotation text goes into table cells and diagram labels escaped for that context. */
  @Test
  void escapesAnnotationTextForTablesAndDiagram() {
    String pkg = "dev.domaincentric.dca.archunit.fixtures.escaping";
    DcaArchitecture escaping =
        DcaArchitecture.of(
            DcaLayout.forBasePackage(pkg), new ClassFileImporter().importPackages(pkg));
    String md = ContextMapRenderer.of(escaping).render();
    assertTrue(md.contains("| billing | Billing | Invoices \\| dunning | — |"), md);
    assertTrue(
        md.contains(
            "| Tax \"Pro\" Service | outbound | — | — | Conformist | implemented |"
                + " Rates come from the provider \\| never computed here |"),
        md);
    assertTrue(md.contains("  ext_tax_pro_service[[\"Tax #quot;Pro#quot; Service\"]]"), md);
    assertTrue(md.contains("  billing -->|\"Conformist / outbound\"| ext_tax_pro_service"), md);
  }

  @Test
  void writeToCreatesParentDirectoriesAndOverwrites(@TempDir Path dir) throws IOException {
    Path target = dir.resolve("docs/architecture/context-map.md");
    Files.createDirectories(dir.resolve("docs"));
    ContextMapRenderer.of(arch).writeTo(target);
    assertTrue(Files.exists(target));
    assertEquals(ContextMapRenderer.of(arch).render(), Files.readString(target));

    ContextMapRenderer.of(arch).withTitle("Other").writeTo(target);
    assertTrue(Files.readString(target).startsWith("# Other"));
  }
}
