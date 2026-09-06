package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.getorder;

import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.Money;

/** Part record named by content, declared beside the result that carries it. */
public record OrderLine(String sku, int quantity, Money lineTotal) {}
