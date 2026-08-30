package dev.domaincentric.dca.buildingblocks.application;

import java.util.function.Supplier;

/**
 * Explicit transaction boundary inside a use case — an application-layer execution abstraction,
 * <b>not</b> an output port.
 *
 * <p>An output port describes a capability the application needs from the outside world (store an
 * aggregate, look up a price, publish an event). A transaction is no such interaction: it defines
 * the execution semantics of several of them. The interface therefore lives beside the use cases
 * and does not extend {@code OutputPort}; the implementation is infrastructure (Spring: {@code
 * TransactionTemplate}).
 *
 * <p>The default boundary is the use case itself ({@code @Transactional}): load, mutate, save,
 * publish — all inside one short transaction. That default breaks down as soon as the use case also
 * talks to the outside world (payment provider, remote catalog, mail gateway): a remote call inside
 * the transaction holds a database connection for the duration of the call, and under load the
 * connection pool runs dry; a rollback after a successful remote call cannot undo the remote effect
 * either.
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
 * <p><b>Nesting.</b> A call inside a running transaction joins it (Spring's {@code REQUIRED}
 * propagation) — there is one commit, at the outermost boundary. A failure in an inner block marks
 * the shared transaction rollback-only even when the outer block catches the exception: the
 * outermost {@code inTransaction} then rolls back and throws instead of committing half of the
 * work. Implementations must preserve this; an in-memory implementation emulates it with a
 * rollback-only flag.
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
   * {@code work} returns and rolls back when it throws.
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
