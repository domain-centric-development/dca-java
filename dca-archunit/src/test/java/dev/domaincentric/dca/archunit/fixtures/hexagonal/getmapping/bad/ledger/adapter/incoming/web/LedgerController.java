package dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.application.postentry.PostEntryCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.application.postentry.PostEntryInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.application.showbalance.ShowBalanceInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.application.showbalance.ShowBalanceQuery;
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
    postEntry.execute(new PostEntryCommand(account));
    return showBalance.execute(new ShowBalanceQuery(account)).reference();
  }

  @GetMapping("/quick-entry")
  public String quickEntry(String account) {
    return book(account);
  }

  private String book(String account) {
    return postEntry.execute(new PostEntryCommand(account)).reference();
  }
}
