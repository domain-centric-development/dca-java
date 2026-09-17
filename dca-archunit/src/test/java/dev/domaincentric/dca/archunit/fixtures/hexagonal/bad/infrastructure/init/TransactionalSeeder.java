package dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.infrastructure.init;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.application.placeorder.PlaceOrderCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.application.placeorder.PlaceOrderInputPort;
import org.springframework.transaction.support.TransactionTemplate;

// Violates LAY-004: infrastructure may wire the transaction manager, but running a transaction
// through a transaction API (role transactionApi) draws a boundary - that belongs to the use case.
public class TransactionalSeeder {
  private final PlaceOrderInputPort placeOrder;
  private final TransactionTemplate transactions;

  public TransactionalSeeder(PlaceOrderInputPort placeOrder, TransactionTemplate transactions) {
    this.placeOrder = placeOrder;
    this.transactions = transactions;
  }

  public void run() {
    transactions.execute(() -> placeOrder.execute(new PlaceOrderCommand(1)));
  }
}
