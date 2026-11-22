package dev.domaincentric.dca.archunit.fixtures.usecase.sharedport.good.alpha.application.install;

import dev.domaincentric.dca.archunit.fixtures.usecase.sharedport.good.alpha.application.shared.Source;

public final class InstallUseCase {
  private final Source source;

  public InstallUseCase(Source source) {
    this.source = source;
  }

  public String run() {
    return source.read();
  }
}
