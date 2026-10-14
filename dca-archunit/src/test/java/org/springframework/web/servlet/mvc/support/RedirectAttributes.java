package org.springframework.web.servlet.mvc.support;

/** Test shim — mirrors the framework type by name only. */
public interface RedirectAttributes {
  RedirectAttributes addAttribute(String name, Object value);

  RedirectAttributes addFlashAttribute(String name, Object value);
}
