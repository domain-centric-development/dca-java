package dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.application.recording;

public class SaveUseCase {
  private dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.application.shared
          .RecordedRepository
      repo;

  public void execute(
      dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model.Recorded value) {
    repo.save(value);
  }
}
