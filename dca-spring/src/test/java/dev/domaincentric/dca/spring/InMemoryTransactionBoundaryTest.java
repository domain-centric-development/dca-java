package dev.domaincentric.dca.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.buildingblocks.application.Refusal;
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

  record Refused() implements Refusal {}

  @Test
  void aReturnedRefusalRollsBackAndIsReturned() {
    Refused refused = new Refused();

    assertSame(refused, boundary.inTransaction(() -> refused));
    assertTrue(boundary.lastBlockRolledBack());
    assertFalse(boundary.isActive());

    boundary.inTransaction(() -> "done");
    assertFalse(boundary.lastBlockRolledBack(), "a success commits");
  }

  @Test
  void aRefusalAfterACaughtInnerFailureDoesNotThrow() {
    Object result =
        boundary.inTransaction(
            () -> {
              try {
                boundary.inTransaction(
                    () -> {
                      throw new IllegalArgumentException("inner failure");
                    });
              } catch (IllegalArgumentException swallowed) {
                // answered as a refusal below
              }
              return new Refused();
            });
    assertInstanceOf(Refused.class, result);
    assertTrue(boundary.lastBlockRolledBack());
  }

  @Test
  void anInnerRefusalFollowedByAnOuterSuccessFails() {
    assertThrows(
        IllegalStateException.class,
        () ->
            boundary.inTransaction(
                () -> {
                  boundary.inTransaction(Refused::new);
                  return "half of the work";
                }));
    assertTrue(boundary.lastBlockRolledBack());
  }

  @Test
  void aFailureCountsAsRolledBack() {
    assertThrows(
        IllegalStateException.class,
        () ->
            boundary.inTransaction(
                () -> {
                  throw new IllegalStateException("boom");
                }));
    assertTrue(boundary.lastBlockRolledBack());
  }
}
