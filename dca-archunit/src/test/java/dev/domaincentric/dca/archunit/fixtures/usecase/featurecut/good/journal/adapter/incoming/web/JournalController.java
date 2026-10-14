package dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.journal.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.journal.application.addnote.AddNoteInputPort;
import dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.journal.application.listnotes.ListNotesInputPort;

/** A flat module has no features to cut along. */
public final class JournalController {
  private final AddNoteInputPort addNote;
  private final ListNotesInputPort listNotes;

  public JournalController(AddNoteInputPort addNote, ListNotesInputPort listNotes) {
    this.addNote = addNote;
    this.listNotes = listNotes;
  }
}
