package dev.domaincentric.dca.spring;

import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * A resource-less {@link AbstractPlatformTransactionManager}: Spring's base class supplies
 * propagation, nesting and rollback-only semantics; this subclass only keeps a per-thread marker
 * for "a transaction is running" and records what happened.
 */
@SuppressWarnings("serial")
final class RecordingTransactionManager extends AbstractPlatformTransactionManager {

  final List<String> log = new ArrayList<>();

  /** Bound to the thread while a transaction runs; carries the rollback-only flag. */
  private static final class Holder {
    boolean rollbackOnly;
  }

  private static final class Tx implements SmartTransactionObject {
    Holder holder;

    @Override
    public boolean isRollbackOnly() {
      return holder != null && holder.rollbackOnly;
    }
  }

  @Override
  protected Object doGetTransaction() {
    Tx tx = new Tx();
    tx.holder = (Holder) TransactionSynchronizationManager.getResource(this);
    return tx;
  }

  @Override
  protected boolean isExistingTransaction(Object transaction) {
    return ((Tx) transaction).holder != null;
  }

  @Override
  protected void doBegin(Object transaction, TransactionDefinition definition) {
    Tx tx = (Tx) transaction;
    tx.holder = new Holder();
    TransactionSynchronizationManager.bindResource(this, tx.holder);
    log.add("begin");
  }

  @Override
  protected void doSetRollbackOnly(DefaultTransactionStatus status) {
    ((Tx) status.getTransaction()).holder.rollbackOnly = true;
  }

  @Override
  protected void doCommit(DefaultTransactionStatus status) {
    log.add("commit");
  }

  @Override
  protected void doRollback(DefaultTransactionStatus status) {
    log.add("rollback");
  }

  @Override
  protected void doCleanupAfterCompletion(Object transaction) {
    TransactionSynchronizationManager.unbindResource(this);
  }
}
