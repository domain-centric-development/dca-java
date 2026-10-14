package dev.domaincentric.dca.archunit.fixtures.naming.trigger.good.archive.application.signon;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface SignOnInputPort extends UseCase<SignOnCommand, SignOnResult> {}
