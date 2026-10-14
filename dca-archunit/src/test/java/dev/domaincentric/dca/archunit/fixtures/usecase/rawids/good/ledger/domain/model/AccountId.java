package dev.domaincentric.dca.archunit.fixtures.usecase.rawids.good.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;
import java.util.UUID;

/** The id's own value type may parse: it is the factory the adapter calls. */
public record AccountId(UUID value) implements Id, Value {
  public static AccountId fromText(String text) {
    return new AccountId(UUID.fromString(text));
  }
}
