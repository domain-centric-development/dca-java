package dev.domaincentric.dca.archunit.fixtures.tactical.factories.good.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/** A value with two origins: built from input (hashed) and rebuilt from storage (taken as is). */
public final class Password implements Value {
  private final String hash;

  private Password(String hash) {
    this.hash = hash;
  }

  public static Password fromPlaintext(String plaintext, Hasher hasher) {
    if (plaintext.length() < 8) {
      throw new IllegalArgumentException("too short");
    }
    return new Password(hasher.hash(plaintext));
  }

  public static Password fromHash(String hash) {
    return new Password(hash);
  }

  public String hash() {
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Password other && other.hash.equals(hash);
  }

  @Override
  public int hashCode() {
    return hash.hashCode();
  }
}
