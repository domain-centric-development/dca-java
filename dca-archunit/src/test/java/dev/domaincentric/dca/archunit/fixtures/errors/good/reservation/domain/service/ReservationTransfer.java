package dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.domain.service;

import dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.domain.model.Reservation;
import dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.domain.model.ReservationStatusInvalidException;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainService;

/** A domain service raises a domain failure: the domain layer refuses, DCA-ERR-009 passes. */
public final class ReservationTransfer implements DomainService {

  public void transfer(Reservation from, Reservation to) {
    if (from.id().equals(to.id())) {
      throw new ReservationStatusInvalidException(from.id());
    }
  }
}
