package dev.domaincentric.dca.archunit.fixtures.naming.trigger.bad.archive.application.archiveentry;

public final class ArchiveEntryOnExpiryUseCase implements ArchiveEntryOnExpiryInputPort {
  @Override
  public ArchiveEntryOnExpiryResult execute(ArchiveEntryOnExpiryCommand input) {
    return new ArchiveEntryOnExpiryResult(input.reference());
  }
}
