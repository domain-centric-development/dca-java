package dev.domaincentric.dca.archunit.fixtures.hexagonal.instancestate.good.ledger.adapter.incoming.web;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** The message for the next page travels in the redirect address as a fixed code. */
public final class LedgerController {
  public String post(RedirectAttributes redirect) {
    redirect.addAttribute("notice", "saved");
    return "redirect:/ledger";
  }
}
