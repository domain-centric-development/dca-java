package dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.journal.application.addnote;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface AddNoteInputPort extends UseCase<AddNoteCommand, AddNoteResult> {}
