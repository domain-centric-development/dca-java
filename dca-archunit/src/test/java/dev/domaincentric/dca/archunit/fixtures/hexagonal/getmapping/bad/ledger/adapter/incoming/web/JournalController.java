package dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.application.postentry.PostEntryCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.application.postentry.PostEntryInputPort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** The class-level mapping names GET; the handler's own mapping names no method and inherits it. */
@Controller
@RequestMapping(value = "/journal", method = RequestMethod.GET)
public final class JournalController {
  private final PostEntryInputPort postEntry;

  public JournalController(PostEntryInputPort postEntry) {
    this.postEntry = postEntry;
  }

  @RequestMapping("/undo")
  public String undo(String account) {
    return postEntry.execute(new PostEntryCommand(account)).reference();
  }
}
