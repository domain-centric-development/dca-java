package dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.application.external;

/** Saves an aggregate that a helper outside its hierarchy registers events on: not exempt. */
public class SaveUseCase {
  private dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.application.shared
          .NoteRepository
      repo;

  public void execute(
      dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model.Note value) {
    repo.save(value);
  }
}
