package dev.domaincentric.dca.archunit.fixtures.persistenceapi.good.ledger.adapter.outgoing;

import dev.domaincentric.dca.archunit.fixtures.persistenceapi.good.ledger.application.Postings;
import java.sql.Connection;
import java.sql.SQLException;

/** Implements the port with the persistence API, where it belongs. */
public class ConnectionPostings implements Postings {

  private final Connection connection;

  public ConnectionPostings(Connection connection) {
    this.connection = connection;
  }

  @Override
  public void add(String posting) {
    try {
      connection.createStatement().execute("select 1");
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }
}
