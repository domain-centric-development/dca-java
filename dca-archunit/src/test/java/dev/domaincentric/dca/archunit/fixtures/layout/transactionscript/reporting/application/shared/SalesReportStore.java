package dev.domaincentric.dca.archunit.fixtures.layout.transactionscript.reporting.application.shared;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Store;
import java.util.List;

/** Output port over data with no aggregate lifecycle — a Store, not a Repository. */
public interface SalesReportStore extends Store {
  List<String> findRecentOrderIds(int limit);
}
