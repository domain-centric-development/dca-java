package dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.application.free;

public class BoundedSaveUseCase {
  private dev.domaincentric.dca.buildingblocks.application.TransactionBoundary boundary;
  private dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.application.shared
          .EntryRepository
      repo;

  public void execute(
      dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model.Entry value) {
    boundary.inTransaction(() -> repo.save(value));
  }
}
