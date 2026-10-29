package dev.domaincentric.dca.archunit.fixtures.roleannotations.app.orders.domain.model;

import dev.domaincentric.dca.archunit.fixtures.roleannotations.vocabulary.Command;

/** A command by the project's annotation, misplaced in the domain. */
@Command
public record PlaceOrderRequest(Amount total) {}
