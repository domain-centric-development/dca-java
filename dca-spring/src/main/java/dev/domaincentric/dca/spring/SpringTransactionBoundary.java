package dev.domaincentric.dca.spring;

import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;
import java.util.function.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link TransactionBoundary} over Spring's {@link TransactionTemplate}.
 *
 * <p>A use case that also talks to the outside world draws its transaction boundary by hand: remote
 * reads before {@link #inTransaction(Supplier)}, the transactional core (load, mutate, save,
 * publish) inside it, remote effects after. The block runs with {@code REQUIRED} propagation, so
 * domain events published inside share the transaction with the save and after-commit listeners
 * fire on commit.
 *
 * <p><b>Nesting.</b> A call inside a running transaction joins it: one commit, at the outermost
 * boundary. A failure in an inner block marks the shared transaction rollback-only even when the
 * outer block catches the exception — the outermost {@code inTransaction} then rolls back and
 * throws {@link org.springframework.transaction.UnexpectedRollbackException} instead of committing
 * half of the work. That is Spring's behaviour; this class does nothing to soften it.
 *
 * <p><b>Prerequisite.</b> A {@link PlatformTransactionManager} bean. The JDBC and JPA starters
 * bring one; an in-memory application has none, and {@code @Transactional} is then silently inert —
 * see {@link DcaSpringAutoConfiguration}.
 */
public class SpringTransactionBoundary implements TransactionBoundary {

  private final TransactionTemplate transactionTemplate;

  public SpringTransactionBoundary(PlatformTransactionManager transactionManager) {
    if (transactionManager == null) {
      throw new IllegalArgumentException("PlatformTransactionManager must not be null");
    }
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @Override
  public <T> T inTransaction(Supplier<T> work) {
    if (work == null) {
      throw new IllegalArgumentException("work must not be null");
    }
    return transactionTemplate.execute(status -> work.get());
  }
}
