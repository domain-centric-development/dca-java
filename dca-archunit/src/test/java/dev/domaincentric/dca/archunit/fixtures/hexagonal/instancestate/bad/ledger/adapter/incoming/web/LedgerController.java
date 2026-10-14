package dev.domaincentric.dca.archunit.fixtures.hexagonal.instancestate.bad.ledger.adapter.incoming.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** The message and the selection stay in the instance that served the request. */
public final class LedgerController {
  public String post(RedirectAttributes redirect) {
    redirect.addFlashAttribute("message", "The entry was saved.");
    return "redirect:/ledger";
  }

  public String select(HttpSession session, String account) {
    session.setAttribute("account", account);
    return "ledger";
  }
}
