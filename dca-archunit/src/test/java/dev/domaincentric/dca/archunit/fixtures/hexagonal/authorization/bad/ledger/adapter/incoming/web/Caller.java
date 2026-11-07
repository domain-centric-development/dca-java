package dev.domaincentric.dca.archunit.fixtures.hexagonal.authorization.bad.ledger.adapter.incoming.web;

/** The request's view of the caller, as the protocol offers it. */
public interface Caller {
  boolean isUserInRole(String role);
}
