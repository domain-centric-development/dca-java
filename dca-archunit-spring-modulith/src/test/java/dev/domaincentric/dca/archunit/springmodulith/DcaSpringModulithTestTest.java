package dev.domaincentric.dca.archunit.springmodulith;

import dev.domaincentric.dca.archunit.DcaLayout;

/** Runs the base class against the fixture — both inherited tests must pass. */
class DcaModulithTestTest extends DcaSpringModulithTest {

  @Override
  protected DcaLayout layout() {
    return DcaLayout.forBasePackage("dev.domaincentric.dca.archunit.springmodulith.fixture");
  }
}
