package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.getorder;

import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.Money;

// DCA-USE-015: the inherited type parameter bound to a value object
public final class OrderTotalResult extends GenericBase<Money> {
  public OrderTotalResult(Money total) {
    super(total);
  }
}
