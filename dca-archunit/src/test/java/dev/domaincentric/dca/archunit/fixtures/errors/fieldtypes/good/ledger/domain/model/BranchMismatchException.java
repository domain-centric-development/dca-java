package dev.domaincentric.dca.archunit.fixtures.errors.fieldtypes.good.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainException;

/** Carries the two codes the rule compared, each as a typed value, and the kind as an enum. */
@SuppressWarnings("serial") // a fixture: what the fields hold is the point, not serialisation
public final class BranchMismatchException extends DomainException {
  private final BranchCode left;
  private final BranchCode right;
  private final EntryKind kind;

  public BranchMismatchException(BranchCode left, BranchCode right, EntryKind kind) {
    super("cannot book " + kind + " across branches " + left.value() + " and " + right.value());
    this.left = left;
    this.right = right;
    this.kind = kind;
  }

  public BranchCode left() {
    return left;
  }

  public BranchCode right() {
    return right;
  }

  public EntryKind kind() {
    return kind;
  }
}
