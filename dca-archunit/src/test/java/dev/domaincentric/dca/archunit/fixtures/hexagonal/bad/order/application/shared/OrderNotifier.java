package dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.application.shared;

// Violates HEX-009: top-level interface in application.shared not extending OutputPort.
public interface OrderNotifier {
  void notifyPlaced(String orderId);
}
