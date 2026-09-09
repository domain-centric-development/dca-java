package dev.domaincentric.dca.archunit.rules;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.InputPort;
import java.lang.reflect.*;
import java.util.*;

/** Shared operation selection and effective input-port surface. */
final class OperationPolicy {
  private OperationPolicy() {}

  static boolean operation(JavaClass type, DcaArchitecture arch) {
    return !type.isInterface()
        && !type.isNestedClass()
        && !type.getModifiers().contains(JavaModifier.ABSTRACT)
        && JavaClass.Predicates.resideInAnyPackage(arch.allApplicationPatterns()).test(type)
        && (type.isAssignableTo(InputPort.class)
            || type.getSimpleName().endsWith(arch.layout().useCaseSuffix()));
  }

  static void invocation(DcaArchitecture arch) {
    var violations = CollectedViolations.withHeader("Use cases must not invoke other use cases");
    for (var caller : arch.classes()) {
      if (!operation(caller, arch)) continue;
      var own = new HashSet<>(caller.getAllRawInterfaces());
      own.addAll(caller.getAllRawSuperclasses());
      own.add(caller);
      var seen = new HashSet<JavaClass>();
      walk(caller, caller, arch, own, seen, new ArrayList<>(), violations);
    }
    violations.throwIfAny();
  }

  private static void walk(
      JavaClass caller,
      JavaClass current,
      DcaArchitecture arch,
      Set<JavaClass> own,
      Set<JavaClass> seen,
      List<String> via,
      CollectedViolations violations) {
    if (!seen.add(current)) return;
    for (var dep : current.getDirectDependenciesFromSelf()) {
      var target = dep.getTargetClass();
      if (own.contains(target)) continue;
      if (target.isAssignableTo(InputPort.class) || operation(target, arch)) {
        violations.add(
            caller.getName()
                + " -> "
                + target.getName()
                + (via.isEmpty() ? "" : " [via " + String.join(" -> ", via) + "]"));
      } else if (!target.isInterface()
          && JavaClass.Predicates.resideInAnyPackage(arch.allApplicationPatterns()).test(target)
          && Objects.equals(
              arch.moduleRootOf(caller.getPackageName()),
              arch.moduleRootOf(target.getPackageName()))) {
        var path = new ArrayList<>(via);
        path.add(target.getName());
        walk(caller, target, arch, own, seen, path, violations);
      }
    }
  }

  static void surface(DcaArchitecture arch) {
    var violations =
        CollectedViolations.withHeader(
            "Use cases expose no public operation outside their input port");
    for (var type : arch.classes()) {
      if (!operation(type, arch)) continue;
      Class<?> runtime = type.reflect();
      Set<String> contracts = new HashSet<>();
      Map<TypeVariable<?>, Type> effectiveBindings = new HashMap<>();
      contracts(runtime, effectiveBindings, contracts);
      Set<String> objectMethods = new HashSet<>();
      for (Method method : Object.class.getMethods())
        objectMethods.add(signature(method, Map.of()));
      for (Method method : runtime.getMethods()) {
        if (Modifier.isStatic(method.getModifiers()) || method.isSynthetic() || method.isBridge())
          continue;
        String signature = signature(method, effectiveBindings);
        if (!objectMethods.contains(signature) && !contracts.contains(signature))
          violations.add(
              type.getName() + " exposes " + method.toGenericString() + " outside its input port");
      }
    }
    violations.throwIfAny();
  }

  private static void contracts(
      Type type, Map<TypeVariable<?>, Type> inherited, Set<String> signatures) {
    if (type == null) return;
    Class<?> raw;
    Map<TypeVariable<?>, Type> bindings = new HashMap<>(inherited);
    if (type instanceof ParameterizedType parameterized) {
      raw = (Class<?>) parameterized.getRawType();
      Type[] args = parameterized.getActualTypeArguments();
      for (int i = 0; i < args.length; i++)
        bindings.put(raw.getTypeParameters()[i], resolve(args[i], inherited));
    } else if (type instanceof Class<?> clazz) raw = clazz;
    else return;
    if (raw.isInterface() && InputPort.class.isAssignableFrom(raw))
      for (Method method : raw.getDeclaredMethods())
        if (!Modifier.isStatic(method.getModifiers())) signatures.add(signature(method, bindings));
    for (Type parent : raw.getGenericInterfaces()) contracts(parent, bindings, signatures);
    contracts(raw.getGenericSuperclass(), bindings, signatures);
    inherited.putAll(bindings);
  }

  private static Type resolve(Type type, Map<TypeVariable<?>, Type> bindings) {
    while (type instanceof TypeVariable<?> variable
        && bindings.containsKey(variable)
        && bindings.get(variable) != type) type = bindings.get(variable);
    return type;
  }

  private static Class<?> erase(Type type, Map<TypeVariable<?>, Type> bindings) {
    type = resolve(type, bindings);
    if (type instanceof Class<?> clazz) return clazz;
    if (type instanceof ParameterizedType p) return (Class<?>) p.getRawType();
    if (type instanceof GenericArrayType a)
      return Array.newInstance(erase(a.getGenericComponentType(), bindings), 0).getClass();
    if (type instanceof TypeVariable<?> v) return erase(v.getBounds()[0], bindings);
    return Object.class;
  }

  private static String signature(Method method, Map<TypeVariable<?>, Type> bindings) {
    return method.getName()
        + Arrays.toString(
            Arrays.stream(method.getGenericParameterTypes())
                .map(t -> erase(t, bindings).getName())
                .toArray());
  }
}
