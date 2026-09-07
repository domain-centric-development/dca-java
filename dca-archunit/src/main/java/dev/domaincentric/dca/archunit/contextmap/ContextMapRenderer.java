package dev.domaincentric.dca.archunit.contextmap;

import com.tngtech.archunit.core.domain.JavaClass;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.ExternalUpstream;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Partnership;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Renders the strategic context map of a {@link DcaArchitecture} as markdown — a fully derived view
 * of the {@code @BoundedContext}, {@code @Upstream}, {@code @ExternalUpstream} and
 * {@code @Partnership} package annotations.
 *
 * <p>Opt-in, not a rule: typical use is a test that regenerates {@code docs/context-map.md} and
 * fails when the committed file was stale.
 *
 * <pre>{@code
 * ContextMapRenderer.of(arch).withTitle("Context Map").writeTo(Path.of("docs/context-map.md"));
 * }</pre>
 */
public final class ContextMapRenderer {

  private static final String NAMED_INTERFACE_ANNOTATION =
      "org.springframework.modulith.NamedInterface";

  private final DcaArchitecture arch;
  private boolean includeExternalSystems = true;
  private boolean includePlanned = true;
  private boolean withMermaid = true;
  private String title = "Context Map";

  private ContextMapRenderer(DcaArchitecture arch) {
    this.arch = Objects.requireNonNull(arch);
  }

  public static ContextMapRenderer of(DcaArchitecture arch) {
    return new ContextMapRenderer(arch);
  }

  /** Whether {@code @ExternalUpstream} declarations are rendered (default {@code true}). */
  public ContextMapRenderer includeExternalSystems(boolean value) {
    this.includeExternalSystems = value;
    return this;
  }

  /** Whether relationships with status {@code PLANNED} are rendered (default {@code true}). */
  public ContextMapRenderer includePlanned(boolean value) {
    this.includePlanned = value;
    return this;
  }

  /** Whether the Mermaid diagram section is rendered (default {@code true}). */
  public ContextMapRenderer withMermaid(boolean value) {
    this.withMermaid = value;
    return this;
  }

  /** The level-one heading (default {@code "Context Map"}). */
  public ContextMapRenderer withTitle(String value) {
    this.title = Objects.requireNonNull(value);
    return this;
  }

  /** Writes the rendered markdown, creating parent directories and overwriting an existing file. */
  public void writeTo(Path file) {
    try {
      Path parent = file.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(file, render());
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot write context map to " + file, e);
    }
  }

  /** Renders the context map as markdown. */
  public String render() {
    Map<String, BoundedContext> contexts = arch.boundedContexts();
    List<String> packages = new ArrayList<>(contexts.keySet());
    packages.sort(Comparator.comparing(arch::contextName));

    StringBuilder md = new StringBuilder();
    md.append("# ").append(title).append("\n\n");
    md.append(
        "> **Generated file — do not edit.** Derived from the `@BoundedContext`, `@Upstream`,\n");
    md.append("> `@ExternalUpstream`, and `@Partnership` package annotations by\n");
    md.append(
        "> `ContextMapRenderer`. After changing a declaration, regenerate and commit this file.\n\n");
    md.append(
        "Each side declares only what it controls: the downstream declares its consumed upstreams\n");
    md.append(
        "(`@Upstream`: translation strategy and channel), the upstream publishes its contract\n");
    md.append(
        "(`api`/`events` named interfaces, `@OpenHostService`), and partnerships are declared\n");
    md.append(
        "symmetrically on both contexts. Organizational patterns such as Customer–Supplier are not\n");
    md.append(
        "machine-classified; Separate Ways is the absence of any declaration. External systems\n");
    md.append(
        "appear via `@ExternalUpstream` on their consuming context — the model dependency always\n");
    md.append(
        "points to the external system, regardless of who initiates the exchange. Non-context\n");
    md.append("modules and the shared kernel are intentionally not part of this map.\n\n");

    md.append("## Bounded Contexts\n\n");
    md.append("| Module | Name | Description | Published interfaces |\n");
    md.append("|---|---|---|---|\n");
    for (String pkg : packages) {
      List<String> published = publishedInterfaces(pkg);
      md.append("| ")
          .append(arch.contextName(pkg))
          .append(" | ")
          .append(cell(contexts.get(pkg).name()))
          .append(" | ")
          .append(cell(contexts.get(pkg).description()))
          .append(" | ")
          .append(published.isEmpty() ? "—" : String.join(", ", published))
          .append(" |\n");
    }

    if (withMermaid) {
      renderDiagram(md, contexts, packages);
    }

    md.append("\n## Upstream relationships\n\n");
    md.append("| Downstream | Upstream | Channel | Translation | Status | Rationale |\n");
    md.append("|---|---|---|---|---|---|\n");
    for (String pkg : packages) {
      String source = arch.contextName(pkg);
      for (Upstream u : upstreams(pkg)) {
        for (Upstream.Consumes channel : u.via()) {
          md.append("| ")
              .append(source)
              .append(" | ")
              .append(u.context())
              .append(" | ")
              .append(channelName(channel))
              .append(" | ")
              .append(translationLabel(u.translation()))
              .append(" | ")
              .append(statusName(u.status()))
              .append(" | ")
              .append(cell(u.rationale()))
              .append(" |\n");
        }
      }
    }

    if (includeExternalSystems) {
      md.append("\n## External systems\n\n");
      boolean anyExternal = packages.stream().anyMatch(p -> !externalUpstreams(p).isEmpty());
      if (!anyExternal) {
        md.append("None declared.\n");
      } else {
        md.append(
            "| Consumer | External system | Interaction | Protocol | Exchanges | Translation |"
                + " Status | Rationale |\n");
        md.append("|---|---|---|---|---|---|---|---|\n");
        for (String pkg : packages) {
          String source = arch.contextName(pkg);
          for (ExternalUpstream e : externalUpstreams(pkg)) {
            md.append("| ")
                .append(source)
                .append(" | ")
                .append(cell(e.name()))
                .append(" | ")
                .append(interactionName(e.interaction()))
                .append(" | ")
                .append(cell(orDash(e.protocol())))
                .append(" | ")
                .append(cell(orDash(e.exchanges())))
                .append(" | ")
                .append(translationLabel(e.translation()))
                .append(" | ")
                .append(statusName(e.status()))
                .append(" | ")
                .append(cell(e.rationale()))
                .append(" |\n");
          }
        }
      }
    }

    md.append("\n## Partnerships\n\n");
    Map<List<String>, List<String>> pairs = partnershipPairs(packages);
    if (pairs.isEmpty()) {
      md.append("None declared.\n");
    } else {
      md.append("| Contexts | Rationale |\n");
      md.append("|---|---|\n");
      pairs.forEach(
          (pair, rationales) ->
              md.append("| ")
                  .append(pair.get(0))
                  .append(" ↔ ")
                  .append(pair.get(1))
                  .append(" | ")
                  .append(cell(String.join(" — ", rationales)))
                  .append(" |\n"));
    }
    return md.toString();
  }

  private void renderDiagram(
      StringBuilder md, Map<String, BoundedContext> contexts, List<String> packages) {
    md.append("\n## Diagram\n\n");
    md.append("```mermaid\ngraph LR\n");
    for (String pkg : packages) {
      md.append("  ")
          .append(arch.contextName(pkg))
          .append("[\"")
          .append(label(contexts.get(pkg).name()))
          .append(publishedBadge(pkg))
          .append("\"]\n");
    }
    md.append("\n");
    for (String pkg : packages) {
      String source = arch.contextName(pkg);
      for (Upstream u : upstreams(pkg)) {
        for (Upstream.Consumes channel : u.via()) {
          String label =
              translationLabel(u.translation())
                  + " / "
                  + channelName(channel)
                  + statusSuffix(u.status());
          String arrow = channel == Upstream.Consumes.API ? "-->" : "-.->";
          md.append("  ")
              .append(source)
              .append(' ')
              .append(arrow)
              .append("|\"")
              .append(label)
              .append("\"| ")
              .append(u.context())
              .append('\n');
        }
      }
    }
    if (includeExternalSystems) {
      for (String name : externalSystems(packages)) {
        md.append("  ")
            .append(externalId(name))
            .append("[[\"")
            .append(label(name))
            .append("\"]]\n");
      }
      for (String pkg : packages) {
        String source = arch.contextName(pkg);
        for (ExternalUpstream e : externalUpstreams(pkg)) {
          // The one-word protocol replaces the generic inbound/outbound in the label — the arrow
          // style already encodes the direction. The full exchanges text lives in the table only.
          String kind =
              e.protocol().isEmpty() ? interactionName(e.interaction()) : label(e.protocol());
          String label =
              translationLabel(e.translation()) + " / " + kind + statusSuffix(e.status());
          String arrow = e.interaction() == ExternalUpstream.Interaction.OUTBOUND ? "-->" : "-.->";
          md.append("  ")
              .append(source)
              .append(' ')
              .append(arrow)
              .append("|\"")
              .append(label)
              .append("\"| ")
              .append(externalId(e.name()))
              .append('\n');
        }
      }
    }
    partnershipPairs(packages)
        .keySet()
        .forEach(
            pair ->
                md.append("  ")
                    .append(pair.get(0))
                    .append(" ---|\"Partnership\"| ")
                    .append(pair.get(1))
                    .append('\n'));
    md.append("```\n\n");
    md.append(
        "Arrows point from downstream to upstream (dependency direction, never call direction).\n");
    md.append(
        "Solid arrows are synchronous consumption (`api` / external `outbound`), dotted arrows are\n");
    md.append(
        "asynchronous consumption (`events` / external `inbound`), plain lines are partnerships.\n");
    md.append("Double-framed nodes are external systems. Node badges list published interfaces.\n");
    md.append("Edges labeled `planned` are declared intent without a code dependency yet.\n");
  }

  // ---------------------------------------------------------------------------------------------
  // Declarations (filtered by options)
  // ---------------------------------------------------------------------------------------------

  private List<Upstream> upstreams(String pkg) {
    return arch.packageAnnotations(pkg, Upstream.class).stream()
        .filter(u -> includePlanned || u.status() != Upstream.Status.PLANNED)
        .toList();
  }

  private List<ExternalUpstream> externalUpstreams(String pkg) {
    return arch.packageAnnotations(pkg, ExternalUpstream.class).stream()
        .filter(e -> includePlanned || e.status() != Upstream.Status.PLANNED)
        .toList();
  }

  /** Deduplicated symmetric pairs (sorted) with the distinct rationales of both sides. */
  private Map<List<String>, List<String>> partnershipPairs(List<String> packages) {
    Map<List<String>, List<String>> pairs = new LinkedHashMap<>();
    for (String pkg : packages) {
      String source = arch.contextName(pkg);
      for (Partnership p : arch.packageAnnotations(pkg, Partnership.class)) {
        List<String> pair = new ArrayList<>(List.of(source, p.context()));
        pair.sort(Comparator.naturalOrder());
        List<String> rationales = pairs.computeIfAbsent(pair, k -> new ArrayList<>());
        if (!p.rationale().isEmpty() && !rationales.contains(p.rationale())) {
          rationales.add(p.rationale());
        }
      }
    }
    return pairs;
  }

  /** All declared external system names, sorted for deterministic output. */
  private List<String> externalSystems(List<String> packages) {
    TreeSet<String> names = new TreeSet<>();
    for (String pkg : packages) {
      externalUpstreams(pkg).forEach(e -> names.add(e.name()));
    }
    return new ArrayList<>(names);
  }

  /**
   * Published interfaces ("api", "events") of a context. Published means declared: the channel
   * package carries classes AND its package-info declares {@code @NamedInterface} with the channel
   * name — a package that merely happens to be called "api" is not a published contract. Without
   * Spring Modulith on the classpath, class presence stands alone.
   */
  private List<String> publishedInterfaces(String contextPackage) {
    List<String> published = new ArrayList<>();
    for (String channel : arch.layout().publishedSubpackages()) {
      // Exact package-segment boundary — a plain prefix would also match "apiary"/"eventsourcing".
      String root = contextPackage + "." + channel;
      boolean hasClasses = false;
      for (JavaClass c : arch.classes()) {
        String p = c.getPackageName();
        if (p.equals(root) || p.startsWith(root + ".")) {
          hasClasses = true;
          break;
        }
      }
      if (hasClasses && declaredAsNamedInterface(root, channel)) {
        published.add(channel);
      }
    }
    return published;
  }

  @SuppressWarnings("unchecked")
  private boolean declaredAsNamedInterface(String channelPackage, String channel) {
    Class<? extends Annotation> namedInterface;
    try {
      // Loaded reflectively so the renderer works unchanged in non-Modulith projects.
      namedInterface = (Class<? extends Annotation>) Class.forName(NAMED_INTERFACE_ANNOTATION);
    } catch (ClassNotFoundException ignored) {
      return true;
    }
    Optional<? extends Annotation> annotation =
        arch.packageAnnotation(channelPackage, namedInterface);
    if (annotation.isEmpty()) {
      return false;
    }
    // Raw reflection sees the attribute that was actually written; the framework's alias bridging
    // between value() and name() only applies through its own annotation utilities.
    List<String> names = new ArrayList<>();
    names.addAll(stringArrayAttribute(annotation.get(), "value"));
    names.addAll(stringArrayAttribute(annotation.get(), "name"));
    return names.contains(channel);
  }

  private static List<String> stringArrayAttribute(Annotation annotation, String attribute) {
    try {
      Method method = annotation.annotationType().getMethod(attribute);
      Object value = method.invoke(annotation);
      return value instanceof String[] ? List.of((String[]) value) : List.of();
    } catch (ReflectiveOperationException e) {
      return List.of();
    }
  }

  /** Published interfaces of a context, shown as a node badge ("api", "events"). */
  private String publishedBadge(String contextPackage) {
    List<String> published = publishedInterfaces(contextPackage);
    return published.isEmpty() ? "" : "<br/><i>" + String.join(" · ", published) + "</i>";
  }

  // ---------------------------------------------------------------------------------------------
  // Labels
  // ---------------------------------------------------------------------------------------------

  /**
   * The mermaid node id of an external system: {@code ext_} plus the name lower-cased in {@link
   * Locale#ROOT} with every run of characters outside {@code [a-z0-9]} replaced by an underscore.
   * Locale-independent, so the rendered map and {@code DCA-MAP-003}'s collision check — which uses
   * this method — agree on every machine.
   */
  public static String externalSystemNodeId(String name) {
    return "ext_" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
  }

  private static String externalId(String name) {
    return externalSystemNodeId(name);
  }

  /** Text inside a markdown table cell: pipes escaped, line breaks flattened. */
  private static String cell(String text) {
    return text.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
  }

  /** Text inside a quoted mermaid label: quotes as entities, line breaks flattened. */
  private static String label(String text) {
    return text.replace("\"", "#quot;").replace("\r", " ").replace("\n", " ");
  }

  private static String interactionName(ExternalUpstream.Interaction interaction) {
    return interaction == ExternalUpstream.Interaction.OUTBOUND ? "outbound" : "inbound";
  }

  private static String translationLabel(Upstream.Translation translation) {
    return translation == Upstream.Translation.ANTI_CORRUPTION_LAYER ? "ACL" : "Conformist";
  }

  private static String statusName(Upstream.Status status) {
    return status == Upstream.Status.PLANNED ? "planned" : "implemented";
  }

  /** Edge-label suffix marking planned relationships; empty for implemented ones. */
  private static String statusSuffix(Upstream.Status status) {
    return status == Upstream.Status.PLANNED ? " / planned" : "";
  }

  private String channelName(Upstream.Consumes channel) {
    return arch.layout().channelSubpackage(channel);
  }

  private static String orDash(String value) {
    return value.isEmpty() ? "—" : value;
  }
}
