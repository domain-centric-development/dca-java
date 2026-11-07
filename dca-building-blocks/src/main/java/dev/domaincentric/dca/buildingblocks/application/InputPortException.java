package dev.domaincentric.dca.buildingblocks.application;

import java.io.Serial;

/**
 * Base class of every failure an input port's contract declares.
 *
 * <p><b>Who declares, raises and catches it.</b> The input port declares it as part of what its
 * operation may answer; the use case behind the port raises it; the incoming adapter that drives
 * the port catches it and turns it into its protocol's answer. Its one concrete branch is {@link
 * UseCaseException}: every failure a use case reports extends that, so this type stays the
 * classification in the hierarchy, not something a project subclasses directly.
 *
 * <p><b>Never caught as such.</b> An incoming adapter that catches {@code InputPortException}
 * answers every outcome the same way; it catches the named subtypes, each with its own answer.
 *
 * @see PortException
 * @see OutputPortException
 */
public abstract class InputPortException extends PortException {

  @Serial private static final long serialVersionUID = 1L;

  /** A failure described in the language of the input port's contract. */
  protected InputPortException(String message) {
    super(message);
  }

  /**
   * A failure described in the language of the input port's contract, raised while handling
   * another.
   */
  protected InputPortException(String message, Throwable cause) {
    super(message, cause);
  }
}
