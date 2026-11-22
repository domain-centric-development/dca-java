package dev.domaincentric.dca.archunit.fixtures.usecase.sharedport.bad.alpha.application.install;

public final class InstallUseCase {
  private final Source source;

  public InstallUseCase(Source source) {
    this.source = source;
  }

  public String run() {
    return source.read();
  }
}
