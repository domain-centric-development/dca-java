package dev.domaincentric.dca.archunit.fixtures.naming.bad.order.application.placeorder;

// DCA-NAM-001: implements a UseCase but does not end with 'UseCase'
public final class PlaceOrderService implements PlaceOrderInputPort {
  @Override
  public PlaceOrderResult execute(PlaceOrderCommand command) {
    return new PlaceOrderResult(command.orderId());
  }
}
