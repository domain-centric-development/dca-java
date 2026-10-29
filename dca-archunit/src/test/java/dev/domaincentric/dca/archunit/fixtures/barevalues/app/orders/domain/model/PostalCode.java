package dev.domaincentric.dca.archunit.fixtures.barevalues.app.orders.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/** A wrapper value whose own error shapes carry the input it refused. */
public record PostalCode(String value) implements Value {

  /** Why a text is no postal code. */
  public sealed interface Error permits EmptyError, TooLongError {}

  /** The refused input was empty. */
  public record EmptyError(String fieldValue) implements Error {}

  /** The refused input was too long. */
  public record TooLongError(String fieldValue, int length) implements Error {}
}
