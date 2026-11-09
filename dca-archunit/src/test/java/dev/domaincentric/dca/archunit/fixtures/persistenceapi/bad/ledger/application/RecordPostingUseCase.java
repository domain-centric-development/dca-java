package dev.domaincentric.dca.archunit.fixtures.persistenceapi.bad.ledger.application;

import java.sql.Connection;
import java.sql.SQLException;

/** Runs its own transaction on a connection. Reported. */
public class RecordPostingUseCase {

  private final Connection connection;

  public RecordPostingUseCase(Connection connection) {
    this.connection = connection;
  }

  public void execute(String posting) throws SQLException {
    connection.setAutoCommit(false);
    connection.commit();
  }
}
