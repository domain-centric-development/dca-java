package dev.domaincentric.dca.archunit.fixtures.roleannotations.vocabulary;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A project's own role annotation, on a type, retained in the class file only. */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Command {}
