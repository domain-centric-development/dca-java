package dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;

public record ReservationId(String value) implements Id {}
