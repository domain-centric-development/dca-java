package dev.domaincentric.dca.archunit.fixtures.hexagonal.portplace.bad.ledger.application.shared;

/** An output port implemented beside its interface, in the application. */
public class ConsoleMailer implements Mailer {
  @Override
  public void send(String to, String text) {}
}
