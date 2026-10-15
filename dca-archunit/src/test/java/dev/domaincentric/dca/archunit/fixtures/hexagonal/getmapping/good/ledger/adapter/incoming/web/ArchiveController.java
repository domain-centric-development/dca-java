package dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.postentry.PostEntryCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.postentry.PostEntryInputPort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * The class-level mapping names POST, so the handler whose own mapping names no method answers POST
 * only; a class-level mapping alone makes no method a handler.
 */
@Controller
@RequestMapping(value = "/archive", method = RequestMethod.POST)
public final class ArchiveController {
  private final PostEntryInputPort postEntry;

  public ArchiveController(PostEntryInputPort postEntry) {
    this.postEntry = postEntry;
  }

  @RequestMapping("/entries")
  public String archive(String account) {
    return postEntry.execute(new PostEntryCommand(account)).reference();
  }

  /** Carries no mapping of its own: not a handler, although the class is mapped. */
  public String helper(String account) {
    return postEntry.execute(new PostEntryCommand(account)).reference();
  }
}
