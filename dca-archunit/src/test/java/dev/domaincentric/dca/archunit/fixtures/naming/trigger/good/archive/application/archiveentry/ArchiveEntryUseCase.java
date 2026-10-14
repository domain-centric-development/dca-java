package dev.domaincentric.dca.archunit.fixtures.naming.trigger.good.archive.application.archiveentry;

public final class ArchiveEntryUseCase implements ArchiveEntryInputPort {
  @Override
  public ArchiveEntryResult execute(ArchiveEntryCommand input) {
    return new ArchiveEntryResult(input.reference());
  }
}
