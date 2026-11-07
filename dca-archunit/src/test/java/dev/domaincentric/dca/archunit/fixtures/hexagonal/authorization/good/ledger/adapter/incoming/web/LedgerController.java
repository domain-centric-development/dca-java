package dev.domaincentric.dca.archunit.fixtures.hexagonal.authorization.good.ledger.adapter.incoming.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/** Declares the claims-only check on the route; its code reads no role. */
@Controller
public class LedgerController {

  @PreAuthorize("hasRole('ACCOUNTANT')")
  public String closeBooks(Caller caller) {
    return "closed";
  }
}
