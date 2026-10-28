package dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.adapter.incoming.api;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.application.postentry.PostEntryCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.application.postentry.PostEntryInputPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** A REST endpoint class whose GET handler drives a command. */
@RestController
public final class LedgerResource {
  private final PostEntryInputPort postEntry;

  public LedgerResource(PostEntryInputPort postEntry) {
    this.postEntry = postEntry;
  }

  @GetMapping("/api/entries/quick")
  public String quick(String account) {
    return postEntry.execute(new PostEntryCommand(account)).reference();
  }
}
