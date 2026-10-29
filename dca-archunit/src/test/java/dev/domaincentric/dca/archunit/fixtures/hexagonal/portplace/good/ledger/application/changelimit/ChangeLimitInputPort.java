package dev.domaincentric.dca.archunit.fixtures.hexagonal.portplace.good.ledger.application.changelimit;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.InputPort;

public interface ChangeLimitInputPort extends InputPort {
  void change(String account, long amount);
}
