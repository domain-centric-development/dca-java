package dev.domaincentric.dca.archunit.fixtures.retrofit.app.billing.application.payinvoice;

import dev.domaincentric.dca.archunit.fixtures.retrofit.app.billing.application.shared.SecretHasher;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class PayInvoiceApplicationService implements PayInvoiceUseCase {
  private final SecretHasher hasher;

  public PayInvoiceApplicationService(SecretHasher hasher) {
    this.hasher = hasher;
  }

  @Override
  public PayStatusResult execute(PayInvoiceCommand command) {
    return hasher.hash(command.secret()).isEmpty() ? PayStatusResult.SHORT : PayStatusResult.PAID;
  }
}
