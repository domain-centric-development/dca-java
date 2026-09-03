package dev.domaincentric.dca.archunit.fixtures.layout.isolation.peer.application.getpeer;

import dev.domaincentric.dca.archunit.fixtures.layout.isolation.catalog.domain.model.Product;

/** Control group: a declared context importing another context's internal domain type. */
public class GetPeerUseCase {
  public String describe(final Product product) {
    return product.sku();
  }
}
