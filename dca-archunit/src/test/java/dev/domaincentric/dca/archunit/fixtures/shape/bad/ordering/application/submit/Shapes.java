package dev.domaincentric.dca.archunit.fixtures.shape.bad.ordering.application.submit;

public final class Shapes {
  public static final class StateCommand {
    private int amount = 1;
  }

  public static final class StateQuery {
    private int amount = 1;
  }

  public static final class StateResult {
    private int amount = 1;
  }

  public record RecordCommand(int amount) {
    public void setAmount(int amount) {}
  }

  public record NamedCommand(int amount) {
    public void setName(String name) {}
  }
}
