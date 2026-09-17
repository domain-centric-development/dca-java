package dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.adapter.incoming.bootstrap;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.application.placeorder.PlaceOrderCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.application.placeorder.PlaceOrderInputPort;
import org.springframework.transaction.support.TransactionTemplate;

// Violates LAY-004: an incoming adapter drawing the transaction boundary itself through a
// programmatic transaction API (the configured transactionApi role) instead of leaving it to the
// use case.
public class SeedRunner {
  private final PlaceOrderInputPort placeOrder;
  private final TransactionTemplate transactions;

  public SeedRunner(PlaceOrderInputPort placeOrder, TransactionTemplate transactions) {
    this.placeOrder = placeOrder;
    this.transactions = transactions;
  }

  public void run() {
    transactions.execute(() -> placeOrder.execute(new PlaceOrderCommand(1)));
  }
}
