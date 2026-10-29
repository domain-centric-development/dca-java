package dev.domaincentric.dca.archunit.fixtures.hexagonal.portplace.bad.ledger.application.shared;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort;

public interface Mailer extends OutputPort {
  void send(String to, String text);
}
