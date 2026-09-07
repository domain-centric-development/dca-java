package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.listorders;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.Order;

/** Not a result itself (no suffix), but a result inherits its aggregate field. */
public abstract class BaseListing {
  protected final Order pinned;

  protected BaseListing(Order pinned) {
    this.pinned = pinned;
  }
}
