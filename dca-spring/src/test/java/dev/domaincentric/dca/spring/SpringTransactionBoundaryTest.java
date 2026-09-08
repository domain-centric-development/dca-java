package dev.domaincentric.dca.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.UnexpectedRollbackException;

class SpringTransactionBoundaryTest {

  private final RecordingTransactionManager manager = new RecordingTransactionManager();
  private final SpringTransactionBoundary boundary = new SpringTransactionBoundary(manager);

  @Test
  void commitsWhenTheWorkReturns() {
    String result = boundary.inTransaction(() -> "done");

    assertEquals("done", result);
    assertEquals(List.of("begin", "commit"), manager.log);
  }

  @Test
  void rollsBackWhenTheWorkThrows() {
    assertThrows(
        IllegalStateException.class,
        () ->
            boundary.inTransaction(
                () -> {
                  throw new IllegalStateException("boom");
                }));

    assertEquals(List.of("begin", "rollback"), manager.log);
  }

  @Test
  void aNestedCallJoinsTheRunningTransaction() {
    boundary.inTransaction(() -> boundary.inTransaction(() -> "inner"));

    assertEquals(List.of("begin", "commit"), manager.log, "one transaction, one commit");
  }

  @Test
  void anInnerFailureCaughtOutsideStillRollsTheWholeTransactionBack() {
    assertThrows(
        UnexpectedRollbackException.class,
        () ->
            boundary.inTransaction(
                () -> {
                  try {
                    boundary.inTransaction(
                        () -> {
                          throw new IllegalStateException("inner failure");
                        });
                  } catch (IllegalStateException swallowed) {
                    // the outer block believes it can continue
                  }
                  return "half of the work";
                }));

    assertEquals(List.of("begin", "rollback"), manager.log);
  }

  @Test
  void rejectsNull() {
    assertThrows(IllegalArgumentException.class, () -> new SpringTransactionBoundary(null));
    assertThrows(
        IllegalArgumentException.class, () -> boundary.inTransaction((Supplier<Object>) null));
  }
}
