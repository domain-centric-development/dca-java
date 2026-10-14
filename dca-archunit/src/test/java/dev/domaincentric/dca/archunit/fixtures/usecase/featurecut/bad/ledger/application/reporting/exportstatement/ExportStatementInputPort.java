package dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.bad.ledger.application.reporting.exportstatement;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface ExportStatementInputPort
    extends UseCase<ExportStatementCommand, ExportStatementResult> {}
