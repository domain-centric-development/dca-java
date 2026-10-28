package dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.adapter.incoming.api;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.showbalance.ShowBalanceInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.showbalance.ShowBalanceQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** A REST endpoint class whose GET handler asks a query. */
@RestController
public final class LedgerResource {
  private final ShowBalanceInputPort showBalance;

  public LedgerResource(ShowBalanceInputPort showBalance) {
    this.showBalance = showBalance;
  }

  @GetMapping("/api/balance")
  public String balance(String account) {
    return showBalance.execute(new ShowBalanceQuery(account)).reference();
  }
}
