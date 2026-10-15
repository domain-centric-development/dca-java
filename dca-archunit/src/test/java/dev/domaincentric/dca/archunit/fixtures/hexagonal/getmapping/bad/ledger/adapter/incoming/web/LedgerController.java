package dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.application.postentry.PostEntryCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.application.postentry.PostEntryInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.application.showbalance.ShowBalanceInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.application.showbalance.ShowBalanceQuery;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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

  @RequestMapping(value = "/reverse", method = RequestMethod.GET)
  public String reverse(String account) {
    return postEntry.execute(new PostEntryCommand(account)).reference();
  }

  /** A general mapping that names no method answers every method, GET included. */
  @RequestMapping("/entries/any")
  public String anyMethod(String account) {
    return postEntry.execute(new PostEntryCommand(account)).reference();
  }

  private String book(String account) {
    return postEntry.execute(new PostEntryCommand(account)).reference();
  }
}
