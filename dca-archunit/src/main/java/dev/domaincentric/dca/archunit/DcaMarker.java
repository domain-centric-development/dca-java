package dev.domaincentric.dca.archunit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * How a role or a layer is recognised in the code: by a supertype, an annotation, a package or a
 * name suffix — or by any of several. A layout names each layer and each role through one marker,
 * so a project that already marks its code its own way points the rules there instead of renaming
 * its code:
 *
 * <pre>{@code
 * DcaLayout.forBasePackage("com.acme.billing")
 *     .withOutgoingAdapter(DcaMarker.anyOf(
 *         DcaMarker.ofPackage("adapter.outgoing"),
 *         DcaMarker.ofAnnotation("com.acme.arch.SecondaryAdapterLayer")));
 * }</pre>
 *
 * <p>Not every kind fits every role. A package says where a type sits, not what it is: a layer is
 * recognised by its package or by an annotation on the package or the type; a building block by a
 * supertype or an annotation on the type, never by its package. The {@code with…} method of a role
 * refuses a kind it does not take.
 *
 * <p>The library ships no annotations of its own for layers or roles: {@link #ofAnnotation(String)}
 * exists so that a project can name the annotations it already has. Every name is a fully qualified
 * type name, matched against the imported classes by name, so the annotation never has to be on the
 * class path of the rules.
 */
public final class DcaMarker {

  /** The ways a marker recognises code. */
  public enum Kind {
    /** Assignable to a type — an interface or a base class. */
    TYPE,
    /** Carries an annotation — on the type, or on its package's {@code package-info}. */
    ANNOTATION,
    /** Lies in a package below a module root, e.g. {@code adapter.outgoing}. */
    PACKAGE,
    /** Has a simple name ending with a suffix — a naming convention, not a marker. */
    SUFFIX,
    /** Any of several markers. */
    ANY_OF
  }

  private final Kind kind;
  private final String value;
  private final List<DcaMarker> members;

  private DcaMarker(Kind kind, String value, List<DcaMarker> members) {
    this.kind = kind;
    this.value = value;
    this.members = members;
  }

  /** Recognises a type assignable to the given interface or class (fully qualified name). */
  public static DcaMarker ofType(String typeName) {
    return new DcaMarker(Kind.TYPE, requireName(typeName, "ofType"), List.of());
  }

  /**
   * Recognises code carrying the given annotation (fully qualified name) — on the type itself or,
   * for a layer, on the {@code package-info} of its package or of a package above it.
   */
  public static DcaMarker ofAnnotation(String annotationName) {
    return new DcaMarker(Kind.ANNOTATION, requireName(annotationName, "ofAnnotation"), List.of());
  }

  /**
   * Recognises code in the given package below a module root — one segment or several, dotted:
   * {@code "domain"}, {@code "adapter.outgoing"}.
   */
  public static DcaMarker ofPackage(String packageBelowModuleRoot) {
    return new DcaMarker(Kind.PACKAGE, requireName(packageBelowModuleRoot, "ofPackage"), List.of());
  }

  /** Recognises a type whose simple name ends with the given suffix. */
  public static DcaMarker ofSuffix(String suffix) {
    return new DcaMarker(Kind.SUFFIX, requireName(suffix, "ofSuffix"), List.of());
  }

  /**
   * Recognises code that any of the given markers recognises — a migration that keeps the old
   * package beside the new annotation, or a project that accepts its own annotation and a
   * library's. Nested {@code anyOf} markers are flattened.
   */
  public static DcaMarker anyOf(DcaMarker... markers) {
    Objects.requireNonNull(markers, "anyOf");
    List<DcaMarker> flat = new ArrayList<>();
    for (DcaMarker marker : markers) {
      Objects.requireNonNull(marker, "anyOf member");
      flat.addAll(marker.alternatives());
    }
    if (flat.isEmpty()) {
      throw new IllegalArgumentException("anyOf needs at least one marker");
    }
    return flat.size() == 1 ? flat.get(0) : new DcaMarker(Kind.ANY_OF, null, List.copyOf(flat));
  }

  public Kind kind() {
    return kind;
  }

  /** The name this marker matches; {@code null} for {@link Kind#ANY_OF}. */
  public String value() {
    return value;
  }

  /** The single markers this one stands for — itself, or the members of an {@code anyOf}. */
  public List<DcaMarker> alternatives() {
    return kind == Kind.ANY_OF ? members : List.of(this);
  }

  /** The values of the alternatives of the given kind, in order. */
  public List<String> valuesOf(Kind wanted) {
    return alternatives().stream().filter(m -> m.kind == wanted).map(m -> m.value).toList();
  }

  /**
   * Refuses this marker unless every alternative is of one of the allowed kinds; the message names
   * the role and the kinds it takes.
   */
  DcaMarker requireKinds(String role, Kind... allowed) {
    List<Kind> kinds = List.of(allowed);
    for (DcaMarker alternative : alternatives()) {
      if (!kinds.contains(alternative.kind)) {
        throw new IllegalArgumentException(
            role
                + " is recognised by "
                + String.join(" or ", Stream.of(allowed).map(k -> "of" + label(k)).toList())
                + ", not by "
                + alternative
                + (alternative.kind == Kind.PACKAGE
                    ? " - a package says where a type sits, not what it is"
                    : ""));
      }
    }
    return this;
  }

  private static String label(Kind kind) {
    return switch (kind) {
      case TYPE -> "Type";
      case ANNOTATION -> "Annotation";
      case PACKAGE -> "Package";
      case SUFFIX -> "Suffix";
      case ANY_OF -> "AnyOf";
    };
  }

  private static String requireName(String value, String factory) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(factory + " needs a name, was blank");
    }
    return value.strip();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof DcaMarker that
        && kind == that.kind
        && Objects.equals(value, that.value)
        && members.equals(that.members);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, value, members);
  }

  /** {@code ofPackage("adapter.outgoing")}, {@code anyOf(ofPackage(..), ofAnnotation(..))}. */
  @Override
  public String toString() {
    if (kind == Kind.ANY_OF) {
      return "anyOf(" + String.join(", ", members.stream().map(DcaMarker::toString).toList()) + ")";
    }
    return "of" + label(kind) + "(" + value + ")";
  }
}
