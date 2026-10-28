package dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.bad.ledger.adapter.incoming.event;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.bad.ledger.application.notifyposting.NotifyPostingCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.bad.ledger.application.notifyposting.NotifyPostingInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.bad.ledger.domain.model.EntryPosted;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Drives a use case synchronously, after the commit without a transaction, and through a helper.
 */
public class PostingNotifier {
  private final NotifyPostingInputPort notifyPosting;

  public PostingNotifier(NotifyPostingInputPort notifyPosting) {
    this.notifyPosting = notifyPosting;
  }

  @EventListener
  void onPosted(EntryPosted event) {
    notifyPosting.execute(new NotifyPostingCommand(event.reference()));
  }

  @TransactionalEventListener
  void afterPosted(EntryPosted event) {
    notifyPosting.execute(new NotifyPostingCommand(event.reference()));
  }

  @EventListener
  void viaHelper(EntryPosted event) {
    send(event.reference());
  }

  private void send(String reference) {
    notifyPosting.execute(new NotifyPostingCommand(reference));
  }
}
