package dev.domaincentric.dca.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InMemoryTransactionBoundaryTest {

  private final InMemoryTransactionBoundary boundary = new InMemoryTransactionBoundary();

  @Test
  void returnsTheResultAndIsInactiveAfterwards() {
    assertEquals(
        "done",
        boundary.inTransaction(
            () -> {
              assertTrue(boundary.isActive());
              return "done";
            }));
    assertFalse(boundary.isActive());
  }

  @Test
  void aNestedCallJoinsAndDoesNotEndTheOuterBlock() {
    boundary.inTransaction(
        () -> {
          boundary.inTransaction(() -> "inner");
          assertTrue(boundary.isActive(), "outer block still running");
          return null;
        });
    assertFalse(boundary.isActive());
  }

  @Test
  void anInnerFailureCaughtOutsideStillFailsTheOutermostBlock() {
    var thrown =
        assertThrows(
            IllegalStateException.class,
            () ->
                boundary.inTransaction(
                    () -> {
                      try {
                        boundary.inTransaction(
                            () -> {
                              throw new IllegalArgumentException("inner failure");
                            });
                      } catch (IllegalArgumentException swallowed) {
                        // caught, as the outer block might
                      }
                      return "half of the work";
                    }));
    assertTrue(thrown.getMessage().contains("rollback-only"));
    assertFalse(boundary.isActive(), "state is reset for the next block");
  }

  @Test
  void aFailurePropagatesAndResetsTheState() {
    assertThrows(
        IllegalStateException.class,
        () ->
            boundary.inTransaction(
                () -> {
                  throw new IllegalStateException("boom");
                }));
    assertFalse(boundary.isActive());
    assertEquals("clean", boundary.inTransaction(() -> "clean"));
  }
}
