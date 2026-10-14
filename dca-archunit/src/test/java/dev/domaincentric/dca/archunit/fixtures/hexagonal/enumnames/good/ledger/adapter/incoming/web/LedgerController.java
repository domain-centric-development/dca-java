package dev.domaincentric.dca.archunit.fixtures.hexagonal.enumnames.good.ledger.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.enumnames.good.ledger.domain.model.AccountStatus;

public final class LedgerController {
  public String status(AccountStatus status) {
    return status.code();
  }

  /** Concatenating the explicit code is fine. */
  public String concatenatedCode(AccountStatus status) {
    return "status=" + status.code();
  }

  /** An enum of the adapter itself may be spelled by name: it is the mapping. */
  public String view(View view) {
    return view.name().toLowerCase(java.util.Locale.ROOT);
  }

  /** Concatenating an enum of the adapter itself is fine as well. */
  public String viewLabel(View view) {
    return "view=" + view;
  }

  enum View {
    LIST,
    DETAIL
  }
}
