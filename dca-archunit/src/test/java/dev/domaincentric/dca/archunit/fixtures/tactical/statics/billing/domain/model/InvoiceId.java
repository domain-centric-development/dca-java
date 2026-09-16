package dev.domaincentric.dca.archunit.fixtures.tactical.statics.billing.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;

public record InvoiceId(String value) implements Id {}
