package dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.lendcopy;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface LendCopyInputPort extends UseCase<LendCopyCommand, LendCopyResult> {}
