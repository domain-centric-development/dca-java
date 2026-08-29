package dev.domaincentric.dca.archunit.fixtures.advanced.good.order.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import java.util.UUID;

public record OrderId(UUID value) implements Id {}
