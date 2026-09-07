package dev.domaincentric.dca.buildingblocks.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/** The default {@code Runnable} overload runs the work through the {@code Supplier} boundary. */
class TransactionBoundaryTest {

  /** Records every block it ran, so the test can see that the Runnable went through it. */
  static final class RecordingBoundary implements TransactionBoundary {
    final List<String> runs = new ArrayList<>();

    @Override
    public <T> T inTransaction(Supplier<T> work) {
      runs.add("begin");
      T result = work.get();
      runs.add("commit");
      return result;
    }
  }

  @Test
  void runnableOverloadDelegatesToTheSupplierBoundary() {
    RecordingBoundary boundary = new RecordingBoundary();
    List<String> sideEffects = new ArrayList<>();

    boundary.inTransaction(() -> sideEffects.add("saved"));

    assertEquals(List.of("begin", "commit"), boundary.runs);
    assertEquals(List.of("saved"), sideEffects);
  }

  @Test
  void supplierOverloadReturnsTheResult() {
    RecordingBoundary boundary = new RecordingBoundary();

    int result = boundary.inTransaction(() -> 42);

    assertEquals(42, result);
    assertTrue(boundary.runs.contains("commit"));
  }
}
