package dev.domaincentric.dca.spring;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Wraps the advice of every transaction advisor in a {@link RefusalRollbackInterceptor}, so that a
 * {@code @Transactional} method that returns a {@link
 * dev.domaincentric.dca.buildingblocks.application.Refusal} rolls back.
 *
 * <p>It acts on the advisor {@code @EnableTransactionManagement} registers (a {@link
 * BeanFactoryTransactionAttributeSourceAdvisor} whose advice is a {@link TransactionInterceptor}).
 * Pointcut and order of the advisor stay as they are; only the advice is wrapped, once.
 * Transactions woven by the AspectJ compiler or load-time weaver have no advisor and are not
 * covered — use {@link SpringTransactionBoundary} there.
 *
 * <p>Registered by {@link DcaSpringAutoConfiguration}; switched off with {@code
 * dca.spring.refusal-rollback.enabled=false}. It is a {@link PriorityOrdered} post-processor
 * because the auto-proxy creator instantiates the advisors as soon as it runs, and a post-processor
 * registered after it would never see them.
 */
public final class RefusalRollbackPostProcessor implements BeanPostProcessor, PriorityOrdered {

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (bean instanceof BeanFactoryTransactionAttributeSourceAdvisor advisor) {
      Advice advice = advisor.getAdvice();
      if (advice instanceof TransactionInterceptor transactionInterceptor) {
        advisor.setAdvice(
            new RefusalRollbackInterceptor((MethodInterceptor) transactionInterceptor));
      }
    }
    return bean;
  }

  @Override
  public int getOrder() {
    return PriorityOrdered.LOWEST_PRECEDENCE;
  }
}
