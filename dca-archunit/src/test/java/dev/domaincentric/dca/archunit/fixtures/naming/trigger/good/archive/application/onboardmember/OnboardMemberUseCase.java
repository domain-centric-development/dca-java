package dev.domaincentric.dca.archunit.fixtures.naming.trigger.good.archive.application.onboardmember;

public final class OnboardMemberUseCase implements OnboardMemberInputPort {
  @Override
  public OnboardMemberResult execute(OnboardMemberCommand input) {
    return new OnboardMemberResult(input.reference());
  }
}
