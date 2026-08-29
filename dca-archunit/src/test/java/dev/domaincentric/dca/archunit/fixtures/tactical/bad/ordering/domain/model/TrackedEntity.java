package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model;

/** Not an Entity itself; its inherited public setter is only visible through getAllMethods. */
public abstract class TrackedEntity {
  private String note;

  public void setNote(String note) {
    this.note = note;
  }

  public String note() {
    return note;
  }
}
