package dev.domaincentric.dca.archunit.fixtures.layout.transactionscript.reporting.application.getsalesreport;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface GetSalesReportInputPort
    extends UseCase<GetSalesReportQuery, GetSalesReportResult> {}
