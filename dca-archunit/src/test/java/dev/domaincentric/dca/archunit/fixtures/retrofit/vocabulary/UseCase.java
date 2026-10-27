package dev.domaincentric.dca.archunit.fixtures.retrofit.vocabulary;

public interface UseCase<I, O> extends InputPort {
  O execute(I input);
}
