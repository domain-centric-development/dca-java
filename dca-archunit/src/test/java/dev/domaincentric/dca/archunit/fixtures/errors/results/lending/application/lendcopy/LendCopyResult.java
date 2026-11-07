package dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.lendcopy;

import dev.domaincentric.dca.buildingblocks.application.Refusal;

/** The answer of the use case: lent, or the expected refusal as a case. */
public sealed interface LendCopyResult permits LendCopyResult.Lent, LendCopyResult.LimitReached {

  record Lent(String loanId) implements LendCopyResult {}

  record LimitReached(int limit) implements LendCopyResult, Refusal {}
}
