package dev.domaincentric.dca.archunit;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Answers an annotation read from the bytecode as a library annotation type. When the imported
 * annotation is the library type itself, ArchUnit's own proxy does it. When a marker role points at
 * a project's own annotation, its members are read by name: a member the library type declares and
 * the project's annotation lacks yields the library default, and a member without a default is
 * reported with the package and the member it was asked for.
 */
final class PackageAnnotations {

  private PackageAnnotations() {}

  static <T extends Annotation> T as(
      Class<T> annotationType, JavaAnnotation<?> annotation, String packageName) {
    if (annotation.getRawType().getName().equals(annotationType.getName())) {
      return annotation.as(annotationType);
    }
    return annotationType.cast(
        Proxy.newProxyInstance(
            annotationType.getClassLoader(),
            new Class<?>[] {annotationType},
            (proxy, method, args) ->
                invoke(annotationType, annotation, packageName, method, args)));
  }

  private static Object invoke(
      Class<? extends Annotation> annotationType,
      JavaAnnotation<?> annotation,
      String packageName,
      Method method,
      Object[] args) {
    switch (method.getName()) {
      case "annotationType":
        return annotationType;
      case "hashCode":
        return annotation.hashCode();
      case "equals":
        return args != null && args.length == 1 && args[0] == annotation;
      case "toString":
        return "@" + annotation.getRawType().getName() + " as @" + annotationType.getName();
      default:
        break;
    }
    Object value = annotation.get(method.getName()).orElse(null);
    if (value == null) {
      Object fallback = method.getDefaultValue();
      if (fallback == null) {
        throw new IllegalStateException(
            "@"
                + annotation.getRawType().getName()
                + " on package "
                + packageName
                + " stands in for @"
                + annotationType.getName()
                + ", whose member "
                + method.getName()
                + "() the rules read - the annotation declares no member of that name. Add it,"
                + " or point the marker role back at the library's annotation.");
      }
      return fallback;
    }
    return convert(value, method.getReturnType(), packageName);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Object convert(Object value, Class<?> type, String packageName) {
    if (value instanceof JavaEnumConstant constant && type.isEnum()) {
      return Enum.valueOf((Class<? extends Enum>) type, constant.name());
    }
    if (value instanceof JavaAnnotation<?> nested && type.isAnnotation()) {
      return as((Class<? extends Annotation>) type, nested, packageName);
    }
    if (value.getClass().isArray() && type.isArray()) {
      Class<?> component = type.getComponentType();
      int length = Array.getLength(value);
      Object converted = Array.newInstance(component, length);
      for (int i = 0; i < length; i++) {
        Array.set(converted, i, convert(Array.get(value, i), component, packageName));
      }
      return converted;
    }
    return value;
  }
}
