package dev.domaincentric.dca.archunit.fixtures.layout.transactionscript.reporting.application.getsalesreport;

import dev.domaincentric.dca.archunit.fixtures.layout.transactionscript.reporting.application.shared.SalesReportStore;
import org.springframework.stereotype.Service;

/** Transaction script: no aggregate, no domain model — read, map, return. */
@Service
public class GetSalesReportUseCase implements GetSalesReportInputPort {

  private final SalesReportStore store;

  public GetSalesReportUseCase(final SalesReportStore store) {
    this.store = store;
  }

  @Override
  public GetSalesReportResult execute(final GetSalesReportQuery query) {
    return new GetSalesReportResult(store.findRecentOrderIds(query.limit()));
  }
}
