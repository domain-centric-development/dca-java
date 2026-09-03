package dev.domaincentric.dca.archunit.fixtures.layout.transactionscript.reporting.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.layout.transactionscript.reporting.application.getsalesreport.GetSalesReportInputPort;
import dev.domaincentric.dca.archunit.fixtures.layout.transactionscript.reporting.application.getsalesreport.GetSalesReportQuery;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SalesReportResource {

  private final GetSalesReportInputPort getSalesReport;

  public SalesReportResource(final GetSalesReportInputPort getSalesReport) {
    this.getSalesReport = getSalesReport;
  }

  public SalesReportResponse recent(final int limit) {
    return new SalesReportResponse(
        getSalesReport.execute(new GetSalesReportQuery(limit)).orderIds());
  }
}
