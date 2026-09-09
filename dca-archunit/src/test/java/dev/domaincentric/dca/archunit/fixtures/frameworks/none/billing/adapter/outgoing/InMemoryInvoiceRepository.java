package dev.domaincentric.dca.archunit.fixtures.frameworks.none.billing.adapter.outgoing;

import dev.domaincentric.dca.archunit.fixtures.frameworks.none.billing.application.shared.InvoiceRepository;
import dev.domaincentric.dca.archunit.fixtures.frameworks.none.billing.domain.model.Invoice;
import dev.domaincentric.dca.archunit.fixtures.frameworks.none.billing.domain.model.InvoiceId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryInvoiceRepository implements InvoiceRepository {
  private final Map<InvoiceId, Invoice> store = new ConcurrentHashMap<>();

  @Override
  public Optional<Invoice> findById(InvoiceId id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public Invoice save(Invoice aggregate) {
    store.put(aggregate.id(), aggregate);
    return aggregate;
  }

  @Override
  public void deleteById(InvoiceId id) {
    store.remove(id);
  }
}
