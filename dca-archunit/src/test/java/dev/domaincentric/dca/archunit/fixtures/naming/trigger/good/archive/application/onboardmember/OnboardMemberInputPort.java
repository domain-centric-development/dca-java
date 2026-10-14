package dev.domaincentric.dca.archunit.fixtures.naming.trigger.good.archive.application.onboardmember;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface OnboardMemberInputPort
    extends UseCase<OnboardMemberCommand, OnboardMemberResult> {}
