package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.shared;

import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.Money;

/** A part record shared between several results: values only. */
public record OrderPart(String sku, int position, Money lineTotal) {}
