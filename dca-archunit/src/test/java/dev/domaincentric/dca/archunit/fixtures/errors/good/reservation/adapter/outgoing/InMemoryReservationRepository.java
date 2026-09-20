package dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.adapter.outgoing;

import dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.application.shared.ReservationRepository;
import dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.domain.model.Reservation;
import dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.domain.model.ReservationId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryReservationRepository implements ReservationRepository {
  private final Map<ReservationId, Reservation> store = new ConcurrentHashMap<>();

  @Override
  public Optional<Reservation> findById(ReservationId id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public Reservation save(Reservation aggregate) {
    store.put(aggregate.id(), aggregate);
    return aggregate;
  }

  @Override
  public void deleteById(ReservationId id) {
    store.remove(id);
  }
}
