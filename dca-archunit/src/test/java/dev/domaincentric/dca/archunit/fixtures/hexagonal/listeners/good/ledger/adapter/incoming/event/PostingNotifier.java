package dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.good.ledger.adapter.incoming.event;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.good.ledger.application.notifyposting.NotifyPostingCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.good.ledger.application.notifyposting.NotifyPostingInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.good.ledger.domain.model.EntryPosted;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/** Drives a use case after the commit, in a transaction of its own. */
public class PostingNotifier {
  private final NotifyPostingInputPort notifyPosting;

  public PostingNotifier(NotifyPostingInputPort notifyPosting) {
    this.notifyPosting = notifyPosting;
  }

  @ApplicationModuleListener
  void onPosted(EntryPosted event) {
    notifyPosting.execute(new NotifyPostingCommand(event.reference()));
  }

  @TransactionalEventListener
  @Transactional
  void afterPosted(EntryPosted event) {
    notifyPosting.execute(new NotifyPostingCommand(event.reference()));
  }
}
