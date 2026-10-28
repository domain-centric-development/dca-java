package dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.good.ledger.application.shared;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort;

public interface PostingMetrics extends OutputPort {
  void count(String reference);
}
