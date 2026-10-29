package dev.domaincentric.dca.archunit.fixtures.hexagonal.portplace.good.ledger.infrastructure;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.portplace.good.ledger.adapter.outgoing.mail.SmtpMailer;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.portplace.good.ledger.application.shared.Mailer;
import java.util.function.Supplier;

/** Wiring only: it refers to the implementation and hands a port a lambda - no class of its own. */
public class MailConfiguration {
  public Mailer mailer() {
    return new SmtpMailer();
  }

  public Mailer silentMailer() {
    return (to, text) -> {};
  }

  public Supplier<Mailer> lazyMailer() {
    return SmtpMailer::new;
  }
}
