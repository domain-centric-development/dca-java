package dev.domaincentric.dca.archunit.fixtures.contextmap.unused.good.billing.domain.model;

import dev.domaincentric.dca.archunit.fixtures.contextmap.unused.good.sharedkernel.domain.model.Money;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record Invoice(String number, Money total) implements Value {}
