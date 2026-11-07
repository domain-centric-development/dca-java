package dev.domaincentric.dca.buildingblocks.application;

import java.util.function.Supplier;

/**
 * Explicit transaction boundary inside a use case — an application-layer execution abstraction,
 * <b>not</b> an output port.
 *
 * <p>An output port describes a capability the application needs from the outside world (store an
 * aggregate, look up a price, publish an event). A transaction is no such interaction: it defines
 * the execution semantics of several of them. The interface therefore lives beside the use cases
 * and does not extend {@code OutputPort}; the implementation is infrastructure - a thin wrapper
 * around the platform's programmatic transaction API (Spring's {@code TransactionTemplate} is one
 * implementation, JTA's {@code UserTransaction} another).
 *
 * <p>The default boundary is the use case itself, declared with the framework's transactional
 * annotation: load, mutate, save, publish — all inside one short transaction. That default breaks
 * down as soon as the use case also talks to the outside world (payment provider, remote catalog,
 * mail gateway): a remote call inside the transaction holds a database connection for the duration
 * of the call, and under load the connection pool runs dry; a rollback after a successful remote
 * call cannot undo the remote effect either.
 *
 * <p>{@code TransactionBoundary} lets the use case draw the boundary by hand — remote reads before,
 * the transactional core inside, remote effects after (preferably as a reaction to an integration
 * event):
 *
 * <pre>{@code
 * Article article = articleDataPort.getArticleData(productId);   // remote read, no transaction
 * return transactionBoundary.inTransaction(() -> {               // short transaction
 *   ShoppingCart cart = carts.findById(cartId).orElseThrow();
 *   cart.addItem(productId, quantity, article.price());
 *   carts.save(cart);
 *   events.publishAndClearEvents(cart);
 *   return AddItemToCartResult.from(cart);
 * });
 * }</pre>
 *
 * <p>Domain events published inside {@link #inTransaction(Supplier)} see the same transaction as
 * the save; after-commit listeners fire when it commits.
 *
 * <p><b>Nesting.</b> A call inside a running transaction joins it (the "required" propagation every
 * transaction manager offers) — there is one commit, at the outermost boundary. A failure in an
 * inner block marks the shared transaction rollback-only even when the outer block catches the
 * exception: the outermost {@code inTransaction} then rolls back and throws instead of committing
 * half of the work. Implementations must preserve this; an in-memory implementation emulates it
 * with a rollback-only flag.
 *
 * <p><b>A returned refusal rolls back.</b> When the work returns a value that is a {@link Refusal}
 * - a case of a closed use-case result that refuses the request - the boundary rolls the
 * transaction back and returns that value unchanged; it does not throw. A refusal returned by the
 * use case therefore undoes every write made before it, exactly as a thrown failure would. Only the
 * value {@code work} returns is looked at, not what it contains: a use case that carries the
 * refusal in a value of its own (a private "saved or refused" type around the result) lets the
 * refusing case of that type implement {@code Refusal} as well. Nested, a refusal behaves like a
 * failure: an inner block that returns one marks the shared transaction rollback-only; the
 * outermost block rolls back cleanly when it returns a refusal too, and throws, like after a caught
 * inner failure, when it returns a success. The outermost block that returns a refusal after an
 * inner failure was caught rolls back without throwing - the refusal is the answer.
 *
 * <p><b>Rules of thumb:</b>
 *
 * <ol>
 *   <li>No remote call inside a transaction — neither in an annotated use case nor inside {@code
 *       inTransaction}.
 *   <li>One aggregate per transaction; cross-aggregate consistency is eventual.
 *   <li>Use the annotation when the whole use case is local; use {@code TransactionBoundary} when
 *       it is not.
 * </ol>
 */
public interface TransactionBoundary {

  /**
   * Runs {@code work} inside one transaction and returns its result; the transaction commits when
   * {@code work} returns, and rolls back when it throws or when the value it returns is a {@link
   * Refusal} - which is then returned unchanged.
   */
  <T> T inTransaction(Supplier<T> work);

  /** Runs {@code work} inside one transaction; convenience for work without a result. */
  default void inTransaction(Runnable work) {
    inTransaction(
        () -> {
          work.run();
          return null;
        });
  }
}
