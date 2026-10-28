package dev.domaincentric.dca.archunit.fixtures.retrofitnoname;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a context but carries no name. */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PACKAGE)
public @interface ContextLabel {
  String value() default "";
}
