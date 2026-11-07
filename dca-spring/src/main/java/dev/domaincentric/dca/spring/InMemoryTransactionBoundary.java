package dev.domaincentric.dca.spring;

import dev.domaincentric.dca.buildingblocks.application.Refusal;
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
 *       IllegalStateException} instead of pretending the work committed;
 *   <li>a block that returns a {@link Refusal} rolls back: the outermost one returns the refusal
 *       without throwing — also after a caught inner failure — and a nested one marks the whole
 *       block rollback-only, as an inner failure does.
 * </ul>
 *
 * <p>Whether the last outermost block on this thread committed or rolled back is answered by {@link
 * #lastBlockRolledBack()}, so a test can assert the roll-back a refusal must cause.
 *
 * <p>The state is per thread, like Spring's transaction synchronization.
 */
public class InMemoryTransactionBoundary implements TransactionBoundary {

  private static final class Frame {
    int depth;
    boolean rollbackOnly;
  }

  private final ThreadLocal<Frame> frame = ThreadLocal.withInitial(Frame::new);
  private final ThreadLocal<Boolean> lastRolledBack = ThreadLocal.withInitial(() -> false);

  @Override
  public <T> T inTransaction(Supplier<T> work) {
    if (work == null) {
      throw new IllegalArgumentException("work must not be null");
    }
    Frame current = frame.get();
    current.depth++;
    try {
      T result = work.get();
      if (result instanceof Refusal) {
        current.rollbackOnly = true;
        if (current.depth == 1) {
          lastRolledBack.set(true);
        }
        return result;
      }
      if (current.depth == 1 && current.rollbackOnly) {
        lastRolledBack.set(true);
        throw new IllegalStateException(
            "Transaction was marked rollback-only by a nested block and cannot commit");
      }
      if (current.depth == 1) {
        lastRolledBack.set(false);
      }
      return result;
    } catch (RuntimeException e) {
      current.rollbackOnly = true;
      if (current.depth == 1) {
        lastRolledBack.set(true);
      }
      throw e;
    } finally {
      current.depth--;
      if (current.depth == 0) {
        frame.remove();
      }
    }
  }

  /**
   * Whether the last outermost block on this thread rolled back — it threw, or it returned a {@link
   * Refusal} — rather than committed. {@code false} before the first block.
   */
  public boolean lastBlockRolledBack() {
    return lastRolledBack.get();
  }

  /** Whether a block is currently running on this thread. */
  public boolean isActive() {
    return frame.get().depth > 0;
  }
}
