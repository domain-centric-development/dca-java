package dev.domaincentric.dca.archunit.fixtures.naming.datawrites.bad.limits.application.updatestatus;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface UpdateStatusInputPort extends UseCase<UpdateStatusCommand, UpdateStatusResult> {}
