package dev.domaincentric.dca.archunit.fixtures.roleannotations.app.orders.domain.model;

import dev.domaincentric.dca.archunit.fixtures.roleannotations.vocabulary.ValueObject;

/** A value by the project's annotation, a wrapper of one component. */
@ValueObject
public record Amount(long cents) {}
