package dev.domaincentric.dca.archunit.fixtures.errors.results.lending.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainException;
import java.io.Serial;

public final class LoanLimitReachedException extends DomainException {

  @Serial private static final long serialVersionUID = 1L;

  public LoanLimitReachedException(int limit) {
    super("loan limit of " + limit + " reached");
  }
}
