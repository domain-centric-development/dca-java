package dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.good.ledger.adapter.incoming.event;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.good.ledger.application.notifyposting.NotifyPostingCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.good.ledger.application.notifyposting.NotifyPostingInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.good.ledger.domain.model.EntryPosted;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.transaction.Transactional;

/** A CDI observer after a successful commit, in a transaction of its own. */
public class CdiPostingNotifier {
  private final NotifyPostingInputPort notifyPosting;

  public CdiPostingNotifier(NotifyPostingInputPort notifyPosting) {
    this.notifyPosting = notifyPosting;
  }

  @Transactional
  void afterPosted(@Observes(during = TransactionPhase.AFTER_SUCCESS) EntryPosted event) {
    notifyPosting.execute(new NotifyPostingCommand(event.reference()));
  }
}
