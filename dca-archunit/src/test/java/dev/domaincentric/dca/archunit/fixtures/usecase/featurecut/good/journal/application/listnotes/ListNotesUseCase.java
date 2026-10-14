package dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.journal.application.listnotes;

public final class ListNotesUseCase implements ListNotesInputPort {
  @Override
  public ListNotesResult execute(ListNotesQuery input) {
    return new ListNotesResult(input.reference());
  }
}
