package dev.domaincentric.dca.archunit.fixtures.advanced.bad.sharedkernel.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;
import java.math.BigDecimal;

public record Money(BigDecimal amount) implements Value {}
