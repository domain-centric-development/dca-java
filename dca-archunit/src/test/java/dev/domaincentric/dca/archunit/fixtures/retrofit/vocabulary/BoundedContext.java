package dev.domaincentric.dca.archunit.fixtures.retrofit.vocabulary;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A project's own context declaration, retained in the class file only. */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PACKAGE)
public @interface BoundedContext {
  String name();

  String description() default "";
}
