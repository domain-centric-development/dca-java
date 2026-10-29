package dev.domaincentric.dca.archunit.fixtures.hexagonal.portplace.bad.ledger.infrastructure;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.portplace.bad.ledger.application.shared.Mailer;

/** An output port implemented in the configuration package instead of an outgoing adapter. */
public class SmtpMailer implements Mailer {
  @Override
  public void send(String to, String text) {}
}
