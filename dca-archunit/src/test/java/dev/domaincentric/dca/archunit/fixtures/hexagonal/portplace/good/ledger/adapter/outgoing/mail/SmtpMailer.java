package dev.domaincentric.dca.archunit.fixtures.hexagonal.portplace.good.ledger.adapter.outgoing.mail;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.portplace.good.ledger.application.shared.Mailer;

public class SmtpMailer implements Mailer {
  @Override
  public void send(String to, String text) {}
}
