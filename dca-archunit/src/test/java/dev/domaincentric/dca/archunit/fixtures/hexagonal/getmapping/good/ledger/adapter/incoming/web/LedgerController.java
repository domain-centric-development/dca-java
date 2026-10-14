package dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.postentry.PostEntryCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.postentry.PostEntryInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.showbalance.ShowBalanceInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.showbalance.ShowBalanceQuery;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public final class LedgerController {
  private final PostEntryInputPort postEntry;
  private final ShowBalanceInputPort showBalance;

  public LedgerController(PostEntryInputPort postEntry, ShowBalanceInputPort showBalance) {
    this.postEntry = postEntry;
    this.showBalance = showBalance;
  }

  @GetMapping("/balance")
  public String show(String account) {
    return showBalance.execute(new ShowBalanceQuery(account)).reference();
  }

  /** Mapped to POST in a real controller; without the GET mapping it is not selected. */
  public String post(String account) {
    return postEntry.execute(new PostEntryCommand(account)).reference();
  }
}
