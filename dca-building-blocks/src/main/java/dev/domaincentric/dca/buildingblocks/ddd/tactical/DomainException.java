package dev.domaincentric.dca.buildingblocks.ddd.tactical;

import java.io.Serial;

/**
 * Base class of every business-rule violation the domain model raises.
 *
 * <p>A domain exception has a name a domain expert would recognise: it says which rule of the model
 * was broken, not which technical operation failed. The test is the name — if the failure has a
 * word in the Ubiquitous Language, it is a domain exception with that word in its name; if it has
 * none, it is not one.
 *
 * <p><b>What is not a domain exception.</b> An argument guard is a programming-error contract, not
 * a business rule: a null check, a range check or a "must not be blank" check in a constructor
 * states what a caller must never pass, and the platform's own argument exception remains the
 * correct answer there. Subclass this type only for a rule the model itself owns — a forbidden
 * state transition, an invariant across several attributes, a quantity the aggregate refuses.
 *
 * <p><b>Where it lives.</b> In the domain layer, beside the model that raises it. It carries no
 * metadata for container management, persistence or transport, and it names no transport vocabulary
 * — no status code, no response shape. Translating a domain exception into a protocol answer is the
 * incoming adapter's work, and the adapter is the only layer that knows the protocol.
 *
 * <p><b>Unchecked on purpose.</b> A broken invariant is not an alternative flow a caller declares
 * in its signature; it travels to the boundary that can answer it.
 *
 * @see dev.domaincentric.dca.buildingblocks.application.UseCaseException
 */
public abstract class DomainException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /** A violation described in the language of the model. */
  protected DomainException(String message) {
    super(message);
  }

  /** A violation described in the language of the model, raised while handling another failure. */
  protected DomainException(String message, Throwable cause) {
    super(message, cause);
  }
}
