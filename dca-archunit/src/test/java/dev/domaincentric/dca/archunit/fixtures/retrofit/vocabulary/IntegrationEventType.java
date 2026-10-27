package dev.domaincentric.dca.archunit.fixtures.retrofit.vocabulary;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface IntegrationEventType {
  String name();

  int version() default 1;
}
