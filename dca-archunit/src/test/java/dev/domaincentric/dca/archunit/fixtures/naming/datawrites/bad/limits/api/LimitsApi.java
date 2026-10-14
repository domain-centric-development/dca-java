package dev.domaincentric.dca.archunit.fixtures.naming.datawrites.bad.limits.api;

/** Published operations named after the field they write. */
public interface LimitsApi {
  void setLimit(String account, long amount);

  void updateStatus(String account, String status);

  long currentLimit(String account);
}
