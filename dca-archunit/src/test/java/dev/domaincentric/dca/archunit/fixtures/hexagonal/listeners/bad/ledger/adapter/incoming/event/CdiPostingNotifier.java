package dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.bad.ledger.adapter.incoming.event;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.bad.ledger.application.notifyposting.NotifyPostingCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.bad.ledger.application.notifyposting.NotifyPostingInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.bad.ledger.domain.model.EntryPosted;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

/** CDI observers: in the raising transaction, and after the commit without a transaction. */
public class CdiPostingNotifier {
  private final NotifyPostingInputPort notifyPosting;

  public CdiPostingNotifier(NotifyPostingInputPort notifyPosting) {
    this.notifyPosting = notifyPosting;
  }

  void onPosted(@Observes EntryPosted event) {
    notifyPosting.execute(new NotifyPostingCommand(event.reference()));
  }

  void afterPosted(@Observes(during = TransactionPhase.AFTER_SUCCESS) EntryPosted event) {
    notifyPosting.execute(new NotifyPostingCommand(event.reference()));
  }

  void afterAnyEnd(@Observes(during = TransactionPhase.AFTER_COMPLETION) EntryPosted event) {
    notifyPosting.execute(new NotifyPostingCommand(event.reference()));
  }
}
