package dev.domaincentric.dca.archunit.fixtures.usecase.sharedport.good.alpha.application.check;

import dev.domaincentric.dca.archunit.fixtures.usecase.sharedport.good.alpha.application.shared.Source;

public final class CheckUseCase {
  private final Source source;

  public CheckUseCase(Source source) {
    this.source = source;
  }

  public String run() {
    return source.read();
  }
}
