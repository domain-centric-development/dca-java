package dev.domaincentric.dca.archunit.fixtures.hexagonal.authorization.bad.ledger.adapter.incoming.web;

import org.springframework.stereotype.Controller;

/** Decides a claims-only check by branching on the caller's role in its own code. */
@Controller
public class LedgerController {

  public String closeBooks(Caller caller) {
    if (!caller.isUserInRole("ACCOUNTANT")) {
      return "forbidden";
    }
    return "closed";
  }
}
