package dev.domaincentric.dca.archunit.fixtures.layout.isolation.catalog.application.describe;

import dev.domaincentric.dca.archunit.fixtures.layout.isolation.reporting.application.getreport.GetReportUseCase;

/**
 * The target side: a declared context reaching into an <em>undeclared</em> module's application
 * layer. Isolation must protect the undeclared module as a target, not only govern it as a source.
 */
public class DescribeProductUseCase {
  public String describe(final GetReportUseCase report) {
    return report.toString();
  }
}
