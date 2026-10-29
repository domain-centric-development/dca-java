package dev.domaincentric.dca.archunit.fixtures.hexagonal.portplace.good.ledger.application.shared;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort;

public interface LimitSetter extends OutputPort {
  void setLimit(String account, long amount);
}
