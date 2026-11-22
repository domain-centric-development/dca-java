package dev.domaincentric.dca.archunit.fixtures.entrypoint.shapes;

/** A field: production code. */
public class WithField {
  static int started;

  public static void main(String[] args) {
    started++;
  }
}
