package dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.bad.ledger.application.reporting.exportstatement;

public final class ExportStatementUseCase implements ExportStatementInputPort {
  @Override
  public ExportStatementResult execute(ExportStatementCommand input) {
    return new ExportStatementResult(input.reference());
  }
}
