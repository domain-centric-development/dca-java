package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.cancelorder;

import java.util.UUID;

// DCA-USE-004: mutable command
public class CancelOrderCommand {
  public UUID orderId;
}
