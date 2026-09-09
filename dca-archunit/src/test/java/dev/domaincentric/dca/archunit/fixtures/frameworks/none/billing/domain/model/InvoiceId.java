package dev.domaincentric.dca.archunit.fixtures.frameworks.none.billing.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import java.util.UUID;

public record InvoiceId(UUID value) implements Id {}
