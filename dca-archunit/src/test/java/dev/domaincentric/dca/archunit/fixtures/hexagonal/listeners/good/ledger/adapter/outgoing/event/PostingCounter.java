package dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.good.ledger.adapter.outgoing.event;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.good.ledger.application.shared.PostingMetrics;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.good.ledger.domain.model.EntryPosted;
import org.springframework.context.event.EventListener;

/** A synchronous listener that calls an output port only. */
public class PostingCounter {
  private final PostingMetrics metrics;

  public PostingCounter(PostingMetrics metrics) {
    this.metrics = metrics;
  }

  @EventListener
  void onPosted(EntryPosted event) {
    metrics.count(event.reference());
  }
}
