package dev.domaincentric.dca.buildingblocks.application;

import java.io.Serial;

/**
 * Base class of every failure an output port's contract declares.
 *
 * <p><b>Who declares, raises and catches it.</b> The output port declares it in its contract, in
 * the application layer beside the port; the outgoing adapter that implements the port raises it,
 * because the contract asks for it; the use case that calls the port catches it and decides the
 * answer - a case of its result where the caller can answer it, or its own {@link
 * UseCaseException}. The adapter raises it, but does not own it: an adapter never invents a failure
 * the application has not declared.
 *
 * <p><b>For an expected failure the port's caller can answer</b> - a uniqueness only the store can
 * see, or a foreign system behind the port that refused or did not answer (a payment provider, a
 * mail provider: an external system on the context map). A failure of the system's own
 * infrastructure - its database, its broker - is no part of the contract and stays the platform's
 * own exception.
 *
 * <p><b>Inside the transaction.</b> The use case may catch it inside its transaction and return a
 * refusal: a returned {@link Refusal} rolls the transaction back like the exception would have.
 *
 * <p><b>Never caught as such.</b> A use case catches the named subtype the port declares, not this
 * base.
 *
 * @see PortException
 * @see InputPortException
 */
public abstract class OutputPortException extends PortException {

  @Serial private static final long serialVersionUID = 1L;

  /** A failure described in the language of the output port's contract. */
  protected OutputPortException(String message) {
    super(message);
  }

  /**
   * A failure described in the language of the output port's contract, raised while handling
   * another - typically the platform's own failure the adapter translated.
   */
  protected OutputPortException(String message, Throwable cause) {
    super(message, cause);
  }
}
