package dev.domaincentric.dca.archunit.fixtures.features.depth.grouped.ordering.application.fulfilment.shiporder;

public record ShipOrderResult(String label) {
  /** A nested helper named like a use case does not define another use case. */
  public static final class LabelUseCase {}
}
