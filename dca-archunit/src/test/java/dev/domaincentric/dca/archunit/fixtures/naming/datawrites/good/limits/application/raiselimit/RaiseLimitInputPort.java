package dev.domaincentric.dca.archunit.fixtures.naming.datawrites.good.limits.application.raiselimit;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface RaiseLimitInputPort extends UseCase<RaiseLimitCommand, RaiseLimitResult> {}
