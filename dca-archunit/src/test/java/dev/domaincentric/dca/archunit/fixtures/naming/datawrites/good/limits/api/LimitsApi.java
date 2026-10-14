package dev.domaincentric.dca.archunit.fixtures.naming.datawrites.good.limits.api;

/** The context's published operations, named after what the change means. */
public interface LimitsApi {
  void raiseLimit(String account, long amount);

  long currentLimit(String account);
}
