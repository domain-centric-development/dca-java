package dev.domaincentric.dca.archunit.fixtures.usecase.sharedport.bad.alpha.application.check;

import dev.domaincentric.dca.archunit.fixtures.usecase.sharedport.bad.alpha.application.install.Source;

public final class CheckUseCase {
  private final Source source;

  public CheckUseCase(Source source) {
    this.source = source;
  }

  public String run() {
    return source.read();
  }
}
