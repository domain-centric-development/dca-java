package dev.domaincentric.dca.archunit.fixtures.entrypoint.shapes;

/** An entry point whose main holds a lambda: the synthetic method belongs to main. */
public final class WithLambda {

  private WithLambda() {}

  public static void main(String[] args) {
    Runnable start = () -> System.out.println(args.length);
    start.run();
  }
}
