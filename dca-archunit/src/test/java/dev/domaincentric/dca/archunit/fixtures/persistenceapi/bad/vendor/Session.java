package dev.domaincentric.dca.archunit.fixtures.persistenceapi.bad.vendor;

/** Stands in for the session type of a persistence library the default list does not name. */
public interface Session {

  void flush();
}
