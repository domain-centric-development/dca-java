package dev.domaincentric.dca.archunit.fixtures.infrastructure.sharedkernel.infrastructure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Shared support in the shared kernel's infrastructure package — everyone may use it. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Lifecycle {}
