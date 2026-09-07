package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.getorder;

// DCA-USE-015: the inherited type parameter bound to a String is a value
public final class OrderNameResult extends GenericBase<String> {
  public OrderNameResult(String name) {
    super(name);
  }
}
