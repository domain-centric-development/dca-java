package dev.domaincentric.dca.archunit.fixtures.collect.sharedkernel.domain.model;

import dev.domaincentric.dca.archunit.fixtures.collect.catalog.api.ProductInfo;
import dev.domaincentric.dca.archunit.fixtures.collect.pricing.api.PriceQuote;

/** DCA-STR-002: the shared kernel depends on two bounded contexts. */
public record Money(ProductInfo product, PriceQuote quote) {}
