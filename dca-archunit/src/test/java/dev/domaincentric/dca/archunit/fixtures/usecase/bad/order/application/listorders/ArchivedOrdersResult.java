package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.listorders;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.Order;

// DCA-USE-015: the aggregate arrives through the inherited field of a base class without suffix
public final class ArchivedOrdersResult extends BaseListing {
  private final int count;

  public ArchivedOrdersResult(Order pinned, int count) {
    super(pinned);
    this.count = count;
  }

  public int count() {
    return count;
  }
}
