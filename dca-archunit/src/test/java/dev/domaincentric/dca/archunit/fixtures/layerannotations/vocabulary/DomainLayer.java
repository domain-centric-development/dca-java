package dev.domaincentric.dca.archunit.fixtures.layerannotations.vocabulary;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A project's own layer annotation, on a package or a type, retained in the class file only. */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.PACKAGE, ElementType.TYPE})
public @interface DomainLayer {}
