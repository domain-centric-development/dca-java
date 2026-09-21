package dev.domaincentric.dca.archunit.rules;

import com.tngtech.archunit.core.domain.JavaClass;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaMarkers;
import java.lang.reflect.*;
import java.util.*;

/** Conservative proof used only to exempt demonstrably event-free saves. */
final class EventFreeAggregate {
  private EventFreeAggregate() {}

  static boolean repository(JavaClass repository, DcaArchitecture arch) {
    DcaMarkers markers = arch.layout().markers();
    Type aggregate;
    try {
      aggregate = aggregate(repository.reflect(), Map.of(), markers);
    } catch (LinkageError | RuntimeException failure) {
      return false;
    }
    if (!(aggregate instanceof Class<?> concrete) || concrete.isInterface()) return false;
    Map<String, JavaClass> scanned = new HashMap<>();
    arch.classes().forEach(c -> scanned.put(c.getName(), c));
    JavaClass current = scanned.get(concrete.getName());
    if (current == null) return false;
    Set<String> hierarchy = new HashSet<>();
    while (current != null && !platform(current.getName(), markers)) {
      if (!scanned.containsKey(current.getName())
          || !noRegistration(current, scanned, new HashSet<>(), markers)) return false;
      hierarchy.add(current.getName());
      current = current.getRawSuperclass().orElse(null);
    }
    return noExternalRegistration(hierarchy, scanned, markers);
  }

  /**
   * A class outside the aggregate's hierarchy that registers an event on it (a same-package helper
   * reaching the protected method) is invisible from the aggregate's own code units, so every
   * scanned class is inspected: a registration whose target is the aggregate or one of its
   * supertypes disables the exemption.
   */
  private static boolean noExternalRegistration(
      Set<String> hierarchy, Map<String, JavaClass> scanned, DcaMarkers markers) {
    for (JavaClass type : scanned.values()) {
      if (hierarchy.contains(type.getName())) continue;
      for (var unit : type.getCodeUnits())
        for (var call : unit.getCallsFromSelf()) {
          var owner = call.getTargetOwner();
          if (call.getTarget().getName().equals("registerEvent")
              && owner.isAssignableTo(markers.aggregateRoot())
              && (hierarchy.contains(owner.getName()) || platform(owner.getName(), markers)))
            return false;
        }
    }
    return true;
  }

  private static boolean noRegistration(
      JavaClass type, Map<String, JavaClass> scanned, Set<String> visited, DcaMarkers markers) {
    if (!visited.add(type.getName())) return true;
    for (var unit : type.getCodeUnits())
      for (var call : unit.getCallsFromSelf()) {
        var owner = call.getTargetOwner();
        if (call.getTarget().getName().equals("registerEvent")
            && owner.isAssignableTo(markers.aggregateRoot())) return false;
        if (platform(owner.getName(), markers)) continue;
        var target = scanned.get(owner.getName());
        if (target == null || !noRegistration(target, scanned, visited, markers)) return false;
      }
    return true;
  }

  /**
   * A type the walk does not enter: the platform's own, or one of the vocabulary's - a marker and
   * the base classes beside it register no event. Derived from the configured roles, so a project's
   * own vocabulary stops the walk where the library's does.
   */
  private static boolean platform(String name, DcaMarkers markers) {
    return name.startsWith("java.")
        || markers.declaresTypesIn(name.substring(0, Math.max(name.lastIndexOf('.'), 0)));
  }

  private static Type aggregate(
      Type type, Map<TypeVariable<?>, Type> inherited, DcaMarkers markers) {
    Class<?> raw;
    Map<TypeVariable<?>, Type> bindings = new HashMap<>(inherited);
    if (type instanceof ParameterizedType p) {
      raw = (Class<?>) p.getRawType();
      for (int i = 0; i < p.getActualTypeArguments().length; i++) {
        Type arg = p.getActualTypeArguments()[i];
        while (arg instanceof TypeVariable<?> v
            && bindings.containsKey(v)
            && bindings.get(v) != arg) arg = bindings.get(v);
        bindings.put(raw.getTypeParameters()[i], arg);
      }
    } else if (type instanceof Class<?> c) raw = c;
    else return null;
    if (raw.getName().equals(markers.repository())) return bindings.get(raw.getTypeParameters()[0]);
    for (Type parent : raw.getGenericInterfaces()) {
      Type found = aggregate(parent, bindings, markers);
      if (found != null) return found;
    }
    return raw.getGenericSuperclass() == null
        ? null
        : aggregate(raw.getGenericSuperclass(), bindings, markers);
  }
}
