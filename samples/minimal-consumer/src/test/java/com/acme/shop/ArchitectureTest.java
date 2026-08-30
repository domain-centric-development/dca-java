package com.acme.shop;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.junit.DcaArchitectureTest;

/**
 * The whole DCA rule catalog, one dynamic test per rule, grouped by rule set.
 *
 * <p>Which rules run and how strictly is configured in {@code
 * src/test/resources/dca-archunit.properties}.
 */
class ArchitectureTest extends DcaArchitectureTest {

  @Override
  protected DcaLayout layout() {
    return DcaLayout.forBasePackage("com.acme.shop");
  }
}
