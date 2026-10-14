package dev.domaincentric.dca.archunit.fixtures.errors.fieldtypes.bad.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainException;

/**
 * Carries two codes as plain strings: nothing in the type tells a code from display text, so both
 * are listed - the remedy is a typed value for the code.
 */
@SuppressWarnings("serial") // a fixture: what the fields hold is the point, not serialisation
public final class BranchMismatchException extends DomainException {
  private final String left;
  private final String right;

  public BranchMismatchException(String left, String right) {
    super("cannot book across branches " + left + " and " + right);
    this.left = left;
    this.right = right;
  }

  public String left() {
    return left;
  }

  public String right() {
    return right;
  }
}
