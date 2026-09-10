package dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model;

/**
 * Registers nothing in its own code units; a nested class the aggregate never calls registers on
 * its behalf. That class lies outside the aggregate hierarchy, so the exemption must not apply. (A
 * helper in another top-level class cannot reach the protected method - this nestmate is the
 * reachable form of "registration from outside the hierarchy".)
 */
public class Note
    extends dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot<Note, EntryId> {
  private final EntryId id;

  public Note(EntryId id) {
    this.id = id;
  }

  public EntryId id() {
    return id;
  }

  public static final class Registrar {
    public void mark(Note note) {
      note.registerEvent(new Changed(java.util.UUID.randomUUID(), java.time.Instant.EPOCH, 1));
    }
  }
}
