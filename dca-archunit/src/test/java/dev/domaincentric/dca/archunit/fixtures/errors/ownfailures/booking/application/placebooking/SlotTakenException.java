package dev.domaincentric.dca.archunit.fixtures.errors.ownfailures.booking.application.placebooking;

/** A failure type of the project's own, not derived from the building blocks' failure markers. */
public final class SlotTakenException extends RuntimeException {
  public SlotTakenException(String slot) {
    super("slot taken: " + slot);
  }
}
