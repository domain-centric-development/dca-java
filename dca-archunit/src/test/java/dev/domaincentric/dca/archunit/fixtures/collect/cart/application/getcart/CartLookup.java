package dev.domaincentric.dca.archunit.fixtures.collect.cart.application.getcart;

import dev.domaincentric.dca.archunit.fixtures.collect.catalog.api.ProductInfo;
import dev.domaincentric.dca.archunit.fixtures.collect.pricing.api.PriceQuote;

/** DCA-MAP-011: depends on two foreign api packages, neither declared. */
public final class CartLookup {
  private final ProductInfo product = new ProductInfo("p1");
  private final PriceQuote quote = new PriceQuote(1);

  public String describe() {
    return product.sku() + quote.cents();
  }
}
