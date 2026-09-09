package dev.domaincentric.dca.archunit.fixtures.shape.good.ordering.application.submit;

public final class Shapes {
  public static final class StateCommand {
    private final int amount = 1;
  }

  public static final class StateQuery {
    private final int amount = 1;
  }

  public static final class StateResult {
    private final int amount = 1;
  }

  public record RecordCommand(int amount) {}
}
