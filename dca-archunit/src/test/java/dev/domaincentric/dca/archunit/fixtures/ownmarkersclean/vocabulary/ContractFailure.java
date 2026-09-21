package dev.domaincentric.dca.archunit.fixtures.ownmarkersclean.vocabulary;

/** A code base's own base class for a broken rule of the model. */
public abstract class ContractFailure extends RuntimeException {

  protected ContractFailure(String message) {
    super(message);
  }
}
