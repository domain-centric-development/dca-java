package com.acme.shop;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.junit.DcaArchitectureTest;
import java.util.Set;

/** The whole DCA rule catalog, one dynamic test per rule. */
class ArchitectureTest extends DcaArchitectureTest {

  @Override
  protected DcaLayout layout() {
    return DcaLayout.forBasePackage("com.acme.shop");
  }

  /** This sample has no DI framework, so the "@Service on use cases" rule does not apply. */
  @Override
  protected Set<String> excludedRuleIds() {
    return Set.of("DCA-NAM-002");
  }
}
