package dev.domaincentric.dca.buildingblocks.application;

import java.io.Serial;

/**
 * Base class of every failure a port's contract declares.
 *
 * <p>A port is a contract between the application and the outside: an input port between the
 * application and the adapter that drives it, an output port between the application and the
 * adapter it drives. A failure that belongs to such a contract is declared by the port, raised on
 * one side of it and caught on the other. Which side is which depends on the direction, so the two
 * kinds have their own bases: {@link InputPortException} - declared by an input port, raised by the
 * use case, caught by the incoming adapter - and {@link OutputPortException} - declared by an
 * output port, raised by the adapter that implements it, caught by the use case.
 *
 * <p><b>Abstract on purpose, and never caught as such.</b> No code raises a {@code PortException}
 * directly, and a caller that catches it, or one of its two direct subclasses, answers every
 * outcome the same way - the named subtypes exist so that each outcome gets its own answer.
 *
 * <p><b>What is not a port failure.</b> A broken rule of the model is a {@link
 * dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainException}; an argument guard stays the
 * platform's own argument exception; a failure of the infrastructure - a connection lost, a timeout
 * - is the platform's own exception and travels to the boundary that can answer it.
 *
 * <p><b>Where it lives.</b> In the application layer, beside the port that declares it. It carries
 * no metadata for container management or transport and names no transport vocabulary.
 */
public abstract class PortException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /** A failure described in the language of the port's contract. */
  protected PortException(String message) {
    super(message);
  }

  /** A failure described in the language of the port's contract, raised while handling another. */
  protected PortException(String message, Throwable cause) {
    super(message, cause);
  }
}
