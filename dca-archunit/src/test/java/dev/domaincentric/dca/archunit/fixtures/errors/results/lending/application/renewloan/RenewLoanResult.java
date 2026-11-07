package dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.renewloan;

import dev.domaincentric.dca.buildingblocks.application.Refusal;

/** A closed result as a sealed class, returned by a port method of its own name. */
public abstract sealed class RenewLoanResult
    permits RenewLoanResult.Renewed, RenewLoanResult.Reserved {

  public static final class Renewed extends RenewLoanResult {}

  public static final class Reserved extends RenewLoanResult implements Refusal {}
}
