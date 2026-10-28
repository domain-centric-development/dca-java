package dev.domaincentric.dca.archunit.fixtures.retrofit.app.billing.adapter.outgoing.persistence;

/** The adapter's own abstraction over its tables - not a port. */
public interface InvoiceRowStore {
  void write(String row);
}
