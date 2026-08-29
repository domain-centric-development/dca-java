package dev.domaincentric.dca.archunit.fixtures.tactical.good.sharedkernel.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;
import java.math.BigDecimal;

public record Money(BigDecimal amount, String currency) implements Value {}
