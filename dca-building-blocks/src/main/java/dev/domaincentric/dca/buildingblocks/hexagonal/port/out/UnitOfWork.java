package dev.domaincentric.dca.buildingblocks.hexagonal.port.out;

import java.util.function.Supplier;

/**
 * Output port for an explicit transaction boundary inside a use case.
 *
 * <p>The default transaction boundary is the use case itself ({@code @Transactional} or the
 * equivalent decorator): load, mutate, save, publish — all inside one short transaction. That
 * default breaks down as soon as the use case also talks to the outside world (payment provider,
 * remote catalog, mail gateway): a remote call inside the transaction holds a database connection
 * for the duration of the call, and under load the connection pool runs dry; a rollback after a
 * successful remote call cannot undo the remote effect either.
 *
 * <p>{@code UnitOfWork} lets the use case draw the boundary by hand — remote reads before, the
 * transactional core inside, remote effects after (preferably as a reaction to an integration
 * event):
 *
 * <pre>{@code
 * Article article = articleDataPort.getArticleData(productId);   // remote read, no transaction
 * return unitOfWork.run(() -> {                                  // short transaction
 *   ShoppingCart cart = carts.findById(cartId).orElseThrow();
 *   cart.addItem(productId, quantity, article.price());
 *   carts.save(cart);
 *   events.publishAndClearEvents(cart);
 *   return AddItemToCartResult.from(cart);
 * });
 * }</pre>
 *
 * <p>The adapter binds the port to the platform's transaction manager (Spring: {@code
 * TransactionTemplate}). Domain events published inside {@link #run(Supplier)} see the same
 * transaction as the save; after-commit listeners fire when it commits.
 *
 * <p><b>Rules of thumb:</b>
 *
 * <ol>
 *   <li>No remote call inside a transaction — neither in an annotated use case nor inside {@code
 *       run}.
 *   <li>One aggregate per transaction; cross-aggregate consistency is eventual.
 *   <li>Use the annotation when the whole use case is local; use {@code UnitOfWork} when it is not.
 * </ol>
 *
 * @see DomainEventPublisher
 * @see Repository
 */
public interface UnitOfWork extends OutputPort {

  /**
   * Runs {@code work} inside one transaction and returns its result; the transaction commits when
   * {@code work} returns and rolls back when it throws.
   */
  <T> T run(Supplier<T> work);

  /** Runs {@code work} inside one transaction; convenience for work without a result. */
  default void perform(Runnable work) {
    run(
        () -> {
          work.run();
          return null;
        });
  }
}
