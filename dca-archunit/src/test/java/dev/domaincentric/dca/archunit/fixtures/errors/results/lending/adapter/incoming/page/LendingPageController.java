package dev.domaincentric.dca.archunit.fixtures.errors.results.lending.adapter.incoming.page;

import dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.lendcopy.LendCopyCommand;
import dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.lendcopy.LendCopyInputPort;
import dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.lendcopy.LendCopyResult;
import dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.renewloan.RenewLoanCommand;
import dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.renewloan.RenewLoanInputPort;
import dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.renewloan.RenewLoanResult;

/**
 * Translates by a type test over the cases of each closed result; names no exception type. The
 * library compiles for Java 17, so this fixture spells with {@code instanceof} what a Java 21
 * {@code switch} spells with case patterns; the switch itself is compiled by the test.
 */
public final class LendingPageController {
  private final LendCopyInputPort lendCopy;
  private final RenewLoanInputPort renewLoan;

  public LendingPageController(LendCopyInputPort lendCopy, RenewLoanInputPort renewLoan) {
    this.lendCopy = lendCopy;
    this.renewLoan = renewLoan;
  }

  public String lend(String memberId, String copyId) {
    LendCopyResult result = lendCopy.execute(new LendCopyCommand(memberId, copyId));
    if (result instanceof LendCopyResult.Lent) {
      return "redirect:/loans";
    }
    return "loan-limit";
  }

  public String renew(String loanId) {
    RenewLoanResult result = renewLoan.renew(new RenewLoanCommand(loanId));
    return result instanceof RenewLoanResult.Renewed ? "renewed" : "reserved";
  }
}
