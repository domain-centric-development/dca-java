package dev.domaincentric.dca.archunit.fixtures.collect.cart.adapter.incoming.web;

import org.springframework.transaction.annotation.Transactional;

/** DCA-LAY-004: a transactional method in an incoming adapter. */
public final class CartController {
  @Transactional
  public void show() {}
}
