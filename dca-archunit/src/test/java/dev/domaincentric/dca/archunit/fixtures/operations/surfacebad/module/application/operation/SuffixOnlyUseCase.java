package dev.domaincentric.dca.archunit.fixtures.operations.surfacebad.module.application.operation;

/** Selected by suffix only: without an InputPort contract every public method is reported. */
public class SuffixOnlyUseCase {
  public String execute(String input) {
    return input;
  }
}
