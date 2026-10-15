package dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.postentry.PostEntryCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.postentry.PostEntryInputPort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/** A class-level mapping that names no method, and no handler on the class: nothing is selected. */
@Controller
@RequestMapping("/catalog")
public final class CatalogController {
  private final PostEntryInputPort postEntry;

  public CatalogController(PostEntryInputPort postEntry) {
    this.postEntry = postEntry;
  }

  public String helper(String account) {
    return postEntry.execute(new PostEntryCommand(account)).reference();
  }
}
