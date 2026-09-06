package dev.domaincentric.dca.archunit.fixtures.cycles.bad.alpha.application.quote;

import dev.domaincentric.dca.archunit.fixtures.cycles.bad.alpha.application.booking.BookingUseCase;

// DCA-CYC-005: quote <-> booking
public final class QuoteUseCase {
  private final BookingUseCase booking = null;
}
