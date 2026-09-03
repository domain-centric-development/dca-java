package dev.domaincentric.dca.archunit.fixtures.layout.transactionscript.reporting.adapter.outgoing.persistence;

import dev.domaincentric.dca.archunit.fixtures.layout.transactionscript.reporting.application.shared.SalesReportStore;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InMemorySalesReportStore implements SalesReportStore {
  @Override
  public List<String> findRecentOrderIds(final int limit) {
    return List.of();
  }
}
