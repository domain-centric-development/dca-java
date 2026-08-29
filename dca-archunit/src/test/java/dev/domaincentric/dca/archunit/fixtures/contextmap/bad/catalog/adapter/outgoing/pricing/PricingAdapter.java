package dev.domaincentric.dca.archunit.fixtures.contextmap.bad.catalog.adapter.outgoing.pricing;

import dev.domaincentric.dca.archunit.fixtures.contextmap.bad.pricing.api.PriceQuote;

public final class PricingAdapter {
  public long cents(PriceQuote quote) {
    return quote.cents();
  }
}
