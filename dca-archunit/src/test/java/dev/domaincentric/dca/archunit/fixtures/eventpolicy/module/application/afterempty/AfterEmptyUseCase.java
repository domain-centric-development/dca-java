package dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.application.afterempty;

public class AfterEmptyUseCase {
  private dev.domaincentric.dca.buildingblocks.application.TransactionBoundary boundary;
  private dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher events;

  public void execute(
      dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model.Recorded value) {
    boundary.inTransaction(() -> {});
    events.publishAndClearEvents(value);
  }
}
