package dev.domaincentric.dca.archunit.modulith.fixture;

import dev.domaincentric.dca.archunit.modulith.fixture.shipping.internal.Label;

/**
 * Stands in for a project's architecture test in the base package. Not a JUnit test — it is a
 * fixture: without the filter, Modulith makes the base package a root module and reports this
 * class's dependency on {@link Label} as a violation. Its nested class must be excluded with it.
 */
public final class FixtureArchitectureTest {

  private FixtureArchitectureTest() {}

  static Label label() {
    return new Label("reaches into shipping internals");
  }

  static final class Helper {
    Label label() {
      return FixtureArchitectureTest.label();
    }
  }
}
