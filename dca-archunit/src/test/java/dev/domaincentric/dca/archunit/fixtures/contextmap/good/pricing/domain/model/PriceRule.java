package dev.domaincentric.dca.archunit.fixtures.contextmap.good.pricing.domain.model;

import dev.domaincentric.dca.archunit.fixtures.contextmap.good.sharedkernel.domain.model.Money;

public record PriceRule(String productId, Money price) {}
