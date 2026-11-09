package dev.domaincentric.dca.archunit.fixtures.persistenceapi.bad.ledger.adapter.outgoing;

import java.sql.Connection;

/** An outgoing adapter may use the persistence API. Not reported. */
public class ConnectionPostings {

  private final Connection connection;

  public ConnectionPostings(Connection connection) {
    this.connection = connection;
  }

  public Connection connection() {
    return connection;
  }
}
