package dev.domaincentric.dca.archunit.rules;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaGenericArrayType;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.core.domain.JavaWildcardType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The one place the rules inspect types: which classes a field or return type involves, which
 * instance fields a class carries, and whether a class declares attribute equality.
 *
 * <p>Type traversal follows ArchUnit's type model — erasure, array component, type arguments and
 * wildcard bounds, recursively — so {@code Map<String, Order>}, {@code Optional<Order>}, {@code
 * Order[]}, {@code List<? extends Order>} and {@code Map<String, List<Order>>} all yield {@code
 * Order}. A field is read in the context of the class that is being inspected: for {@code class
 * Base<T> { T value; }} and {@code class OrderResult extends Base<Order>}, the inherited field
 * involves {@code Order}, not {@code Object} — the type arguments of every superclass on the way
 * from the inspected class to the field's owner are substituted. The rules decide which of the
 * involved types are forbidden, not how to find them.
 */
final class TypeInspection {

  private static final String OBJECT = "java.lang.Object";

  private TypeInspection() {}

  /**
   * Every raw class a type involves: the erasure, array component types and, recursively, all
   * generic type arguments and wildcard bounds. Type variables contribute their bounds. In
   * encounter order, without duplicates.
   */
  static List<JavaClass> involvedTypes(JavaType type) {
    Set<JavaClass> involved = new LinkedHashSet<>();
    collect(type, Map.of(), involved, new HashSet<>());
    return new ArrayList<>(involved);
  }

  /**
   * Every raw class a field involves as seen from {@code viewedFrom}, which declares or inherits
   * the field: a type variable of the field's owner is replaced by the type argument the subclass
   * chain binds it to, through any number of levels and inside containers. An unbound variable
   * contributes its bounds.
   */
  static List<JavaClass> involvedTypes(JavaField field, JavaClass viewedFrom) {
    Set<JavaClass> involved = new LinkedHashSet<>();
    collect(field.getType(), typeArgumentBindings(viewedFrom), involved, new HashSet<>());
    return new ArrayList<>(involved);
  }

  /**
   * The instance fields of a class, inherited ones included, sorted by name so that reports are
   * stable across runs. Static fields — constants, counters — are not part of an object's state.
   */
  static List<JavaField> instanceFields(JavaClass type) {
    return type.getAllFields().stream()
        .filter(field -> !field.getModifiers().contains(JavaModifier.STATIC))
        .sorted(Comparator.comparing(JavaField::getName))
        .toList();
  }

  /**
   * Whether the class overrides both {@code boolean equals(Object)} and {@code int hashCode()} —
   * exactly those signatures. An overload such as {@code equals(Money)} does not count: the class
   * still compares by identity through the inherited {@code Object.equals(Object)}.
   */
  static boolean declaresAttributeEquality(JavaClass type) {
    return overrides(type, "equals", "boolean", OBJECT) && overrides(type, "hashCode", "int");
  }

  /**
   * What each superclass's type variables are bound to, walking up from {@code type}: for {@code
   * OrderResult extends Intermediate<Order>} and {@code Intermediate<U> extends Base<List<U>>} the
   * map holds {@code Intermediate.U -> Order} and {@code Base.T -> List<U>}; {@link #collect}
   * resolves the chain when it meets {@code U} inside {@code List<U>}.
   */
  private static Map<String, JavaType> typeArgumentBindings(JavaClass type) {
    Map<String, JavaType> bindings = new HashMap<>();
    Optional<JavaType> superclass = type.getSuperclass();
    while (superclass.isPresent()) {
      JavaType current = superclass.get();
      JavaClass erasure = current.toErasure();
      if (current instanceof JavaParameterizedType parameterized) {
        List<JavaTypeVariable<JavaClass>> parameters = erasure.getTypeParameters();
        List<JavaType> arguments = parameterized.getActualTypeArguments();
        for (int i = 0; i < Math.min(parameters.size(), arguments.size()); i++) {
          bindings.put(variableKey(parameters.get(i)), arguments.get(i));
        }
      }
      superclass = erasure.getSuperclass();
    }
    return bindings;
  }

  private static void collect(
      JavaType type,
      Map<String, JavaType> bindings,
      Set<JavaClass> involved,
      Set<String> resolving) {
    if (type instanceof JavaTypeVariable<?> variable) {
      String key = variableKey(variable);
      JavaType bound = bindings.get(key);
      if (bound != null && resolving.add(key)) {
        collect(bound, bindings, involved, resolving);
        resolving.remove(key);
      } else {
        involved.addAll(type.getAllInvolvedRawTypes());
      }
    } else if (type instanceof JavaParameterizedType parameterized) {
      involved.add(parameterized.toErasure());
      for (JavaType argument : parameterized.getActualTypeArguments()) {
        collect(argument, bindings, involved, resolving);
      }
    } else if (type instanceof JavaWildcardType wildcard) {
      for (JavaType bound : wildcard.getUpperBounds()) {
        collect(bound, bindings, involved, resolving);
      }
      for (JavaType bound : wildcard.getLowerBounds()) {
        collect(bound, bindings, involved, resolving);
      }
    } else if (type instanceof JavaGenericArrayType array) {
      collect(array.getComponentType(), bindings, involved, resolving);
    } else {
      involved.addAll(type.getAllInvolvedRawTypes());
    }
  }

  /** A type variable is identified by its owner and name; {@code T} of two classes differ. */
  private static String variableKey(JavaTypeVariable<?> variable) {
    Object owner = variable.getOwner();
    String ownerName =
        owner instanceof JavaClass javaClass
            ? javaClass.getName()
            : owner instanceof JavaMethod method ? method.getFullName() : String.valueOf(owner);
    return ownerName + "#" + variable.getName();
  }

  private static boolean overrides(
      JavaClass type, String name, String returnType, String... parameterTypes) {
    return type.getAllMethods().stream()
        .anyMatch(m -> isOverride(m, name, returnType, parameterTypes));
  }

  private static boolean isOverride(
      JavaMethod method, String name, String returnType, String... parameterTypes) {
    if (!method.getName().equals(name)
        || method.getOwner().getName().equals(OBJECT)
        || method.getModifiers().contains(JavaModifier.STATIC)
        || !method.getRawReturnType().getName().equals(returnType)) {
      return false;
    }
    List<String> actual = method.getRawParameterTypes().stream().map(JavaClass::getName).toList();
    return actual.equals(List.of(parameterTypes));
  }
}
