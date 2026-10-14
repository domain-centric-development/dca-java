package dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.journal.application.addnote;

public final class AddNoteUseCase implements AddNoteInputPort {
  @Override
  public AddNoteResult execute(AddNoteCommand input) {
    return new AddNoteResult(input.reference());
  }
}
