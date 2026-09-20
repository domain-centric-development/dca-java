package dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;

public final class Reservation extends BaseAggregateRoot<Reservation, ReservationId> {
  private final ReservationId id;
  private boolean confirmed;

  public Reservation(ReservationId id) {
    if (id == null) {
      throw new IllegalArgumentException("id must not be null");
    }
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
