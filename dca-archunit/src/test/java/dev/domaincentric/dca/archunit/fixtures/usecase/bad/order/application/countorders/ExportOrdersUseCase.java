package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.countorders;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

// DCA-USE-022: binds a bare text as the output itself.
public final class ExportOrdersUseCase implements UseCase<CountOrdersQuery, String> {

  @Override
  public String execute(CountOrdersQuery query) {
    return query.region();
  }
}
