package dev.domaincentric.dca.spring;

import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;
import java.util.function.Supplier;

/**
 * {@link TransactionBoundary} for tests and for the in-memory phase of an application — no
 * transaction manager, no Spring, but the same nesting contract.
 *
 * <p>Nothing is atomic here: in-memory repositories mutate maps directly, so a failure does not
 * undo earlier writes. What this class does preserve is the <em>shape</em> of the contract, so a
 * use case that works against it behaves the same once {@link SpringTransactionBoundary} replaces
 * it:
 *
 * <ul>
 *   <li>a nested call joins the running block — one "commit", at the outermost boundary;
 *   <li>a failure in an inner block marks the whole block rollback-only; if the outer block catches
 *       the exception and returns normally, the outermost {@code inTransaction} throws {@link
 *       IllegalStateException} instead of pretending the work committed.
 * </ul>
 *
 * <p>The state is per thread, like Spring's transaction synchronization.
 */
public class InMemoryTransactionBoundary implements TransactionBoundary {

  private static final class Frame {
    int depth;
    boolean rollbackOnly;
  }

  private final ThreadLocal<Frame> frame = ThreadLocal.withInitial(Frame::new);

  @Override
  public <T> T inTransaction(Supplier<T> work) {
    if (work == null) {
      throw new IllegalArgumentException("work must not be null");
    }
    Frame current = frame.get();
    current.depth++;
    try {
      T result = work.get();
      if (current.depth == 1 && current.rollbackOnly) {
        throw new IllegalStateException(
            "Transaction was marked rollback-only by a nested block and cannot commit");
      }
      return result;
    } catch (RuntimeException e) {
      current.rollbackOnly = true;
      throw e;
    } finally {
      current.depth--;
      if (current.depth == 0) {
        frame.remove();
      }
    }
  }

  /** Whether a block is currently running on this thread. */
  public boolean isActive() {
    return frame.get().depth > 0;
  }
}
