package dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.application.free;

public class SaveUseCase {
  private dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.application.shared
          .EntryRepository
      repo;

  public void execute(
      dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model.Entry value) {
    repo.save(value);
  }
}
