package dev.domaincentric.dca.archunit.fixtures.layout.greenfield.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import java.util.UUID;

public record OrderId(UUID value) implements Id {}
