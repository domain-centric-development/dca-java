package dev.domaincentric.dca.archunit.fixtures.errors.results.lending.adapter.incoming.rest;

import dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.returncopy.ReturnCopyCommand;
import dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.returncopy.ReturnCopyInputPort;

/** Drives a use case with neither a failure handler nor a switch over a closed result. */
public final class LendingRestController {
  private final ReturnCopyInputPort returnCopy;

  public LendingRestController(ReturnCopyInputPort returnCopy) {
    this.returnCopy = returnCopy;
  }

  public String returnCopy(String loanId) {
    try {
      return returnCopy.execute(new ReturnCopyCommand(loanId)).loanId();
    } catch (RuntimeException anything) {
      return "500";
    }
  }
}
