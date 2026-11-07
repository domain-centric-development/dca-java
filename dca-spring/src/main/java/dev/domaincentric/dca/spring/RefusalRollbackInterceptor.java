package dev.domaincentric.dca.spring;

import dev.domaincentric.dca.buildingblocks.application.Refusal;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * Rolls back a {@code @Transactional} method that returns a {@link Refusal}, exactly as if it had
 * thrown.
 *
 * <p>Spring's {@link org.springframework.transaction.interceptor.TransactionInterceptor} commits
 * whatever a method returns; only an exception rolls back. A use case that answers an expected
 * refusal as a case of its result would commit every write it made before it found out it has to
 * refuse. This interceptor wraps the transaction interceptor and looks at the value the method
 * returns <em>while the transaction is still open</em>: a {@code Refusal} marks the current
 * transaction rollback-only, and the transaction interceptor then rolls back instead of committing.
 * The refusal is returned unchanged; nothing is thrown.
 *
 * <p><b>Why it wraps instead of being an advisor of its own.</b> A second advisor only runs inside
 * the transaction when it is ordered after the transaction advisor, and Spring Boot registers the
 * transaction advisor with the lowest precedence there is — two advisors of equal order have no
 * defined order. Wrapping the transaction advisor's own advice puts the check exactly between the
 * method and the commit, whatever else is ordered around it. {@link RefusalRollbackPostProcessor}
 * does the wrapping.
 *
 * <p><b>Marked locally, rolled back cleanly.</b> The mark is set on the method's own transaction
 * status. A method that begins the transaction rolls back without an {@link
 * org.springframework.transaction.UnexpectedRollbackException} — also when an inner transactional
 * call failed and was caught before, which is the case a use case meets when it catches a failure
 * thrown across a transactional proxy and answers it as a refusal. A method that joins an outer
 * transaction marks the shared transaction rollback-only, as an inner failure would; an outer
 * method that returns a refusal as well rolls back cleanly, one that returns a success throws.
 */
public final class RefusalRollbackInterceptor implements MethodInterceptor {

  private final MethodInterceptor transactionInterceptor;

  /** Wraps the transaction interceptor whose methods are to roll back on a returned refusal. */
  public RefusalRollbackInterceptor(MethodInterceptor transactionInterceptor) {
    if (transactionInterceptor == null) {
      throw new IllegalArgumentException("transactionInterceptor must not be null");
    }
    this.transactionInterceptor = transactionInterceptor;
  }

  /** The wrapped transaction interceptor. */
  public MethodInterceptor transactionInterceptor() {
    return transactionInterceptor;
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    return transactionInterceptor.invoke(new RollingBackOnRefusal(invocation));
  }

  /** The method invocation as the transaction interceptor sees it; looks at what it returns. */
  private record RollingBackOnRefusal(MethodInvocation delegate) implements MethodInvocation {

    @Override
    public Object proceed() throws Throwable {
      Object result = delegate.proceed();
      if (result instanceof Refusal) {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      }
      return result;
    }

    @Override
    public Method getMethod() {
      return delegate.getMethod();
    }

    @Override
    public Object[] getArguments() {
      return delegate.getArguments();
    }

    @Override
    public Object getThis() {
      return delegate.getThis();
    }

    @Override
    public AccessibleObject getStaticPart() {
      return delegate.getStaticPart();
    }
  }
}
