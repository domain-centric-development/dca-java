package dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.postentry.PostEntryCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.postentry.PostEntryInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.showbalance.ShowBalanceInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.showbalance.ShowBalanceQuery;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    return showBalance.execute(new ShowBalanceQuery(account)).reference();
  }

  /** Mapped to POST in a real controller; without the GET mapping it is not selected. */
  public String post(String account) {
    return postEntry.execute(new PostEntryCommand(account)).reference();
  }

  /** POST through the general mapping: the attribute names no GET. */
  @RequestMapping(value = "/entries", method = RequestMethod.POST)
  public String postEntry(String account) {
    return postEntry.execute(new PostEntryCommand(account)).reference();
  }

  /** POST through a mapping whose meta-annotation names the method. */
  @PostMapping("/entries/quick")
  public String quickEntry(String account) {
    return postEntry.execute(new PostEntryCommand(account)).reference();
  }

  /** A general mapping without a method attribute is not selected (documented in the rule). */
  @RequestMapping("/entries/any")
  public String anyMethod(String account) {
    return postEntry.execute(new PostEntryCommand(account)).reference();
  }
}
