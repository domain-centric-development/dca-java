package dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.application.shared;

import dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.domain.model.Reservation;
import dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.domain.model.ReservationId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;

public interface ReservationRepository extends Repository<Reservation, ReservationId> {}
