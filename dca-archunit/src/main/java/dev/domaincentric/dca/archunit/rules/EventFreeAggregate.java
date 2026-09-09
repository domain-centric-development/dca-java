package dev.domaincentric.dca.archunit.rules;

import com.tngtech.archunit.core.domain.JavaClass;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.AggregateRoot;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;
import java.lang.reflect.*;
import java.util.*;

/** Conservative proof used only to exempt demonstrably event-free saves. */
final class EventFreeAggregate {
  private EventFreeAggregate() {}

  static boolean repository(JavaClass repository, DcaArchitecture arch) {
    Type aggregate;
    try {
      aggregate = aggregate(repository.reflect(), Map.of());
    } catch (LinkageError | RuntimeException failure) {
      return false;
    }
    if (!(aggregate instanceof Class<?> concrete) || concrete.isInterface()) return false;
    Map<String, JavaClass> scanned = new HashMap<>();
    arch.classes().forEach(c -> scanned.put(c.getName(), c));
    JavaClass current = scanned.get(concrete.getName());
    if (current == null) return false;
    while (current != null && !platform(current.getName())) {
      if (!scanned.containsKey(current.getName())
          || !noRegistration(current, scanned, new HashSet<>())) return false;
      current = current.getRawSuperclass().orElse(null);
    }
    return true;
  }

  private static boolean noRegistration(
      JavaClass type, Map<String, JavaClass> scanned, Set<String> visited) {
    if (!visited.add(type.getName())) return true;
    for (var unit : type.getCodeUnits())
      for (var call : unit.getCallsFromSelf()) {
        var owner = call.getTargetOwner();
        if (call.getTarget().getName().equals("registerEvent")
            && owner.isAssignableTo(AggregateRoot.class)) return false;
        if (platform(owner.getName())) continue;
        var target = scanned.get(owner.getName());
        if (target == null || !noRegistration(target, scanned, visited)) return false;
      }
    return true;
  }

  private static boolean platform(String name) {
    return name.startsWith("java.") || name.startsWith("dev.domaincentric.dca.buildingblocks.");
  }

  private static Type aggregate(Type type, Map<TypeVariable<?>, Type> inherited) {
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
    if (raw == Repository.class) return bindings.get(raw.getTypeParameters()[0]);
    for (Type parent : raw.getGenericInterfaces()) {
      Type found = aggregate(parent, bindings);
      if (found != null) return found;
    }
    return raw.getGenericSuperclass() == null
        ? null
        : aggregate(raw.getGenericSuperclass(), bindings);
  }
}
