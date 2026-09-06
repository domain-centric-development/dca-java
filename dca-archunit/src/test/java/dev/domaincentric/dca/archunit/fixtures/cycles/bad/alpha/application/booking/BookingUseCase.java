package dev.domaincentric.dca.archunit.fixtures.cycles.bad.alpha.application.booking;

import dev.domaincentric.dca.archunit.fixtures.cycles.bad.alpha.application.quote.QuoteUseCase;

public final class BookingUseCase {
  private final QuoteUseCase quote = null;
}
