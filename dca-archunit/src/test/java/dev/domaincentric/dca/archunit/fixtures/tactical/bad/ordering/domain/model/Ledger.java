package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** DCA-TAC-008: identities hidden in a map, an optional and a nested container. */
public record Ledger(
    Map<String, Order> ordersBySku, Optional<Shipment> lastShipment, List<List<Customer>> customers)
    implements Value {}
