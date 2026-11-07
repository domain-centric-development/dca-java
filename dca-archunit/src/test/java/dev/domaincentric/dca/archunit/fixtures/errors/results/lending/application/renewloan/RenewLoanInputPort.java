package dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.renewloan;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.InputPort;

public interface RenewLoanInputPort extends InputPort {
  RenewLoanResult renew(RenewLoanCommand command);
}
