package dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.ledger.adapter.incoming.web.booking;

import dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.ledger.application.booking.postentry.PostEntryInputPort;

public final class BookingController {
  private final PostEntryInputPort postEntry;

  public BookingController(PostEntryInputPort postEntry) {
    this.postEntry = postEntry;
  }
}
