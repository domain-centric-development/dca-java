package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.application.shared;

import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.Customer;
import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.CustomerId;
import java.util.Optional;

/** DCA-TAC-013: named *Repository without extending the marker. */
public interface CustomerRepository {
  Optional<Customer> findById(CustomerId id);
}
