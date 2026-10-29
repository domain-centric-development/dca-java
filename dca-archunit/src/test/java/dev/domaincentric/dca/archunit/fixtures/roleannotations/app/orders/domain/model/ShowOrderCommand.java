package dev.domaincentric.dca.archunit.fixtures.roleannotations.app.orders.domain.model;

/** Named like a command, but the project marks its commands by annotation - a read. */
public record ShowOrderCommand(Amount total) {}
