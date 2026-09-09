package io.micronaut.data.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Test shim — mirrors the framework annotation by name only. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface MappedEntity {}
