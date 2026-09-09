package dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.application.unresolved;

public class SaveUseCase<
    T extends
        dev.domaincentric.dca.buildingblocks.ddd.tactical.AggregateRoot<
                T,
                dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model.EntryId>> {
  private dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository<
          T, dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model.EntryId>
      repo;

  public void execute(T value) {
    repo.save(value);
  }
}
