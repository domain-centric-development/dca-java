package dev.domaincentric.dca.archunit.fixtures.usecase.sharedport.good.alpha.application.own;

public final class OwnUseCase {
  private final OwnStore ownStore;

  public OwnUseCase(OwnStore ownStore) {
    this.ownStore = ownStore;
  }

  public String run() {
    return ownStore.read();
  }
}
