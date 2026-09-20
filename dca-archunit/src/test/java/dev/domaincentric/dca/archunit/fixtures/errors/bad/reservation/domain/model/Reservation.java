package dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;

public final class Reservation extends BaseAggregateRoot<Reservation, ReservationId> {
  private final ReservationId id;
  private boolean confirmed;

  public Reservation(ReservationId id) {
    this.id = id;
  }

  public void confirm() {
    if (confirmed) {
      throw new ReservationAlreadyConfirmedException(id);
    }
    confirmed = true;
  }

  @Override
  public ReservationId id() {
    return id;
  }
}
