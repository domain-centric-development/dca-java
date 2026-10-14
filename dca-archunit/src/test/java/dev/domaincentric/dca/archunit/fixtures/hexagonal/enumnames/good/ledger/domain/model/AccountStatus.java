package dev.domaincentric.dca.archunit.fixtures.hexagonal.enumnames.good.ledger.domain.model;

/** A value from a fixed set with a code per value. */
public enum AccountStatus {
  OPEN("open"),
  CLOSED("closed");

  private final String code;

  AccountStatus(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }
}
