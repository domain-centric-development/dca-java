package dev.domaincentric.dca.archunit.fixtures.retrofit.vocabulary;

public interface NoResultUseCase<I> extends InputPort {
  void execute(I input);
}
