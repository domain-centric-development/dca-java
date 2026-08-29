package org.springframework.modulith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Test shim — mirrors the framework annotation by name only. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE, ElementType.TYPE})
public @interface ApplicationModule {
  String[] allowedDependencies() default {};

  String displayName() default "";
}
